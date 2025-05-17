package com.example.routerconfandmonitor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddRouterActivity extends AppCompatActivity {

    private EditText nameInput, ipInput, usernameInput, passwordInput;
    private Button saveRouterButton;
    private ImageView settingsIcon;
    private FirebaseAuth mAuth;

    private static final String CHANNEL_ID = "router_ops";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_router);

        mAuth = FirebaseAuth.getInstance();

        nameInput = findViewById(R.id.routerNameInput);
        ipInput = findViewById(R.id.routerIpInput);
        usernameInput = findViewById(R.id.routerUsernameInput);
        passwordInput = findViewById(R.id.routerPasswordInput);
        saveRouterButton = findViewById(R.id.saveRouterButton);
        settingsIcon = findViewById(R.id.settingsIcon);

        // Pre-fill IP from intent
        String prefilledIp = getIntent().getStringExtra("routerIp");
        if (prefilledIp != null) {
            ipInput.setText(prefilledIp);
        }

        // ✅ Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        saveRouterButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String ip = ipInput.getText().toString().trim();
            String user = usernameInput.getText().toString().trim();
            String pass = passwordInput.getText().toString().trim();

            if (name.isEmpty() || ip.isEmpty()) {
                Toast.makeText(this, "Name and IP address are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                String uid = currentUser.getUid();
                try {
                    String encryptedPass = SecureEncryptionUtil.encrypt(pass);
                    Router router = new Router(name, ip, user, encryptedPass, uid);

                    db.collection("users").document(uid).collection("routers")
                            .add(router)
                            .addOnSuccessListener(documentReference -> {
                                showNotification("Router saved", "New router has been added.");
                                new android.os.Handler().postDelayed(() -> finish(), 500);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save router: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } catch (Exception e) {
                    Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ☰ gear icon menu
        settingsIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(AddRouterActivity.this, settingsIcon);
            popupMenu.getMenu().add("Dashboard");
            popupMenu.getMenu().add("Routers");
            popupMenu.getMenu().add("Log out");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                switch (title) {
                    case "Dashboard":
                        startActivity(new Intent(AddRouterActivity.this, DashboardActivity.class));
                        finish();
                        return true;
                    case "Routers":
                        startActivity(new Intent(AddRouterActivity.this, RouterListActivity.class));
                        finish();
                        return true;
                    case "Log out":
                        mAuth.signOut();
                        startActivity(new Intent(AddRouterActivity.this, MainActivity.class));
                        finish();
                        return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    private void showNotification(String title, String message) {
        // ✅ Ellenőrző visszajelzés
        //Toast.makeText(this, "🔔 Notification triggered!", Toast.LENGTH_SHORT).show();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Router Operations",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
