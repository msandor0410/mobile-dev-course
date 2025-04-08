package com.example.routerconfandmonitor;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText ipInput;
    private Button connectButton, refreshButton;
    private TextView statusText, signalText, ipAddressText;
    private ImageView settingsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);

        mAuth = FirebaseAuth.getInstance();

        ipInput = findViewById(R.id.ipInput);
        connectButton = findViewById(R.id.connectButton);
        refreshButton = findViewById(R.id.refreshButton);
        statusText = findViewById(R.id.statusValue);
        signalText = findViewById(R.id.signalValue);
        ipAddressText = findViewById(R.id.ipValue);
        settingsIcon = findViewById(R.id.settingsIcon);

        connectButton.setOnClickListener(v -> {
            statusText.setText("Connected");
            signalText.setText("Strong");
            ipAddressText.setText(ipInput.getText().toString());
        });

        refreshButton.setOnClickListener(v -> {
            // Frissítési logika
        });

        settingsIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(DashboardActivity.this, settingsIcon);
            popupMenu.getMenu().add("Log out");
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Log out")) {
                    mAuth.signOut();
                    startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }
}
