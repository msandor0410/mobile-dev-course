// DashboardActivity.java
package com.example.routerconfandmonitor;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText ipInput;
    private Button pingButton;
    private TextView pingOutput;
    private ImageView settingsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard_activity);

        mAuth = FirebaseAuth.getInstance();

        ipInput = findViewById(R.id.ipInput);
        pingButton = findViewById(R.id.pingButton);
        pingOutput = findViewById(R.id.pingOutput);
        settingsIcon = findViewById(R.id.settingsIcon);

        if (pingOutput != null) {
            pingOutput.setMovementMethod(new ScrollingMovementMethod());
        }

        if (pingButton != null) {
            pingButton.setOnClickListener(v -> {
                String ip = ipInput.getText().toString().trim();
                if (ip.isEmpty()) {
                    Toast.makeText(this, getString(R.string.enter_ip_warning), Toast.LENGTH_SHORT).show();
                    return;
                }
                runPing(ip);
            });
        }

        if (settingsIcon != null) {
            settingsIcon.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(DashboardActivity.this, settingsIcon);
                popupMenu.getMenu().add(getString(R.string.menu_routers));
                popupMenu.getMenu().add(getString(R.string.menu_wifi_scan));
                popupMenu.getMenu().add(getString(R.string.menu_logout));

                popupMenu.setOnMenuItemClickListener(item -> {
                    String title = item.getTitle() != null ? item.getTitle().toString() : "";
                    switch (title) {
                        case "Routers":
                        case "Routerek":
                            startActivity(new Intent(this, RouterListActivity.class));
                            return true;
                        case "Wi-Fi Scan":
                            startActivity(new Intent(this, WifiScanActivity.class));
                            return true;
                        case "Log out":
                            mAuth.signOut();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                            return true;
                    }
                    return false;
                });
                popupMenu.show();
            });
        }
    }

    private void runPing(String ip) {
        if (pingOutput == null) return;
        pingOutput.setText(getString(R.string.pinging, ip));
        new Thread(() -> {
            StringBuilder output = new StringBuilder();

            try {
                Process process = Runtime.getRuntime().exec("ping -c 4 " + ip);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();
            } catch (Exception e) {
                output.append(getString(R.string.ping_error, e.getMessage())).append("\n");
            }

            runOnUiThread(() -> pingOutput.setText(output.toString()));

            new Thread(() -> {
                StringBuilder httpOutput = new StringBuilder();
                httpOutput.append("\n").append(getString(R.string.https_check)).append("\n");

                try {
                    java.net.URL url = new java.net.URL("https://" + ip);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.connect();
                    int responseCode = conn.getResponseCode();
                    httpOutput.append(getString(R.string.http_ok)).append("\n")
                            .append(getString(R.string.http_status, responseCode));
                } catch (Exception ex) {
                    httpOutput.append(getString(R.string.http_fail)).append("\n").append(ex.getMessage());
                }

                runOnUiThread(() -> pingOutput.append("\n\n" + httpOutput));
            }).start();

        }).start();
    }
}
