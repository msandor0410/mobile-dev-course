package com.example.routerconfandmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditRouterActivity extends AppCompatActivity {

    private EditText nameInput, ipInput, usernameInput, passwordInput;
    private Button saveChangesButton;
    private ImageView settingsIcon;
    private FirebaseAuth mAuth;
    private String routerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_router);

        mAuth = FirebaseAuth.getInstance();

        nameInput = findViewById(R.id.routerNameInput);
        ipInput = findViewById(R.id.routerIpInput);
        usernameInput = findViewById(R.id.routerUsernameInput);
        passwordInput = findViewById(R.id.routerPasswordInput);
        saveChangesButton = findViewById(R.id.saveRouterButton);
        settingsIcon = findViewById(R.id.settingsIcon);

        Intent intent = getIntent();
        routerId = intent.getStringExtra("routerId");
        nameInput.setText(intent.getStringExtra("routerName"));
        ipInput.setText(intent.getStringExtra("routerIp"));
        usernameInput.setText(intent.getStringExtra("routerUsername"));
        passwordInput.setText(intent.getStringExtra("routerPassword"));

        saveChangesButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String ip = ipInput.getText().toString().trim();
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (name.isEmpty() || ip.isEmpty()) {
                Toast.makeText(this, "Name and IP address are required!", Toast.LENGTH_SHORT).show();
                return;
            }

            String encryptedPassword;
            try {
                encryptedPassword = SecureEncryptionUtil.encrypt(password);
            } catch (Exception e) {
                Toast.makeText(this, "Encryption failed!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();

            if (user != null && routerId != null) {
                db.collection("users").document(user.getUid()).collection("routers")
                        .document(routerId)
                        .update(
                                "name", name,
                                "ip", ip,
                                "username", username,
                                "password", encryptedPassword  // ✅ titkosított jelszó
                        )
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Router updated successfully!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to update router", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        settingsIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(EditRouterActivity.this, settingsIcon);
            popupMenu.getMenu().add("Dashboard");
            popupMenu.getMenu().add("Routers");
            popupMenu.getMenu().add("Log out");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                switch (title) {
                    case "Dashboard":
                        startActivity(new Intent(EditRouterActivity.this, DashboardActivity.class));
                        finish();
                        return true;
                    case "Routers":
                        startActivity(new Intent(EditRouterActivity.this, RouterListActivity.class));
                        finish();
                        return true;
                    case "Log out":
                        mAuth.signOut();
                        startActivity(new Intent(EditRouterActivity.this, MainActivity.class));
                        finish();
                        return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }
}
