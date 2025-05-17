
package com.example.routerconfandmonitor;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class RouterListActivity extends AppCompatActivity {

    private ImageView settingsIcon;
    private FirebaseAuth mAuth;
    private Button addRouterButton;
    private TextView noRoutersText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.router_connection_activity);

        mAuth = FirebaseAuth.getInstance();

        settingsIcon = findViewById(R.id.settingsIcon);
        addRouterButton = findViewById(R.id.addRouterButton);
        noRoutersText = findViewById(R.id.noRoutersText);

        // ✅ Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        settingsIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(RouterListActivity.this, settingsIcon);
            popupMenu.getMenu().add("Dashboard");
            popupMenu.getMenu().add("Log out");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals("Dashboard")) {
                    startActivity(new Intent(RouterListActivity.this, DashboardActivity.class));
                    finish();
                    return true;
                } else if (title.equals("Log out")) {
                    mAuth.signOut();
                    startActivity(new Intent(RouterListActivity.this, MainActivity.class));
                    finish();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });

        addRouterButton.setOnClickListener(v -> {
            startActivity(new Intent(RouterListActivity.this, AddRouterActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(this, "Welcome to your routers!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRouters();
    }

    private void loadRouters() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        LinearLayout routerListLayout = findViewById(R.id.routerListLayout);
        routerListLayout.removeAllViews(); // clear list

        if (user != null) {
            db.collection("users").document(user.getUid()).collection("routers")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            noRoutersText.setVisibility(TextView.VISIBLE);
                        } else {
                            noRoutersText.setVisibility(TextView.GONE);
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Router r = doc.toObject(Router.class);
                                String docId = doc.getId();

                                TextView routerView = new TextView(this);
                                routerView.setText(r.getName() + " – " + r.getIp());
                                routerView.setTextSize(16);
                                routerView.setTextColor(getResources().getColor(android.R.color.black));
                                routerView.setPadding(16, 16, 16, 16);

                                routerView.setOnClickListener(view -> {
                                    PopupMenu menu = new PopupMenu(this, routerView);
                                    menu.getMenu().add("Connect");
                                    menu.getMenu().add("Edit");
                                    menu.getMenu().add("Delete");

                                    menu.setOnMenuItemClickListener(item -> {
                                        String title = item.getTitle().toString();
                                        switch (title) {
                                            case "Connect":
                                                try {
                                                    String decryptedPass = SecureEncryptionUtil.decrypt(r.getPassword());

                                                    new AlertDialog.Builder(this)
                                                            .setTitle("Router adatok")
                                                            .setMessage("IP: " + r.getIp()
                                                                    + "\nUsername: " + r.getUsername()
                                                                    + "\nPassword: " + decryptedPass)
                                                            .setPositiveButton("Open in browser", (dialog, which) -> {
                                                                String url = "http://" + r.getIp();
                                                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                                                                startActivity(browserIntent);
                                                            })
                                                            .setNegativeButton("Cancel", null)
                                                            .show();
                                                } catch (Exception e) {
                                                    Toast.makeText(this, "Password decryption failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                                return true;
                                            case "Edit":
                                                Intent editIntent = new Intent(this, EditRouterActivity.class);
                                                editIntent.putExtra("routerId", docId);
                                                editIntent.putExtra("routerName", r.getName());
                                                editIntent.putExtra("routerIp", r.getIp());
                                                editIntent.putExtra("routerUsername", r.getUsername());
                                                editIntent.putExtra("routerPassword", r.getPassword());
                                                startActivity(editIntent);
                                                return true;
                                            case "Delete":
                                                confirmAndDeleteRouter(docId);
                                                return true;
                                        }
                                        return false;
                                    });

                                    menu.show();
                                });

                                routerListLayout.addView(routerView);
                            }
                            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
                            routerListLayout.startAnimation(fadeIn);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading routers", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void confirmAndDeleteRouter(String docId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db.collection("users").document(user.getUid()).collection("routers")
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        showNotification("Router deleted", "A router was successfully removed.");
                        loadRouters();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete router", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("router_ops", "Router Operations", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "router_ops")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
