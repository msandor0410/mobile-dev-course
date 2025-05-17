package com.example.routerconfandmonitor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class WifiScanActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
    private WifiManager wifiManager;
    private ListView wifiListView;
    private ArrayAdapter<String> adapter;
    private List<ScanResult> scanResults;
    private ImageView settingsIcon;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_scan);

        wifiListView = findViewById(R.id.wifiListView);
        settingsIcon = findViewById(R.id.settingsIcon);
        mAuth = FirebaseAuth.getInstance();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        scanResults = new ArrayList<>();

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wi-Fi is disabled. Please enable it manually.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
        } else {
            startWifiScan();
        }

        settingsIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(WifiScanActivity.this, settingsIcon);
            popupMenu.getMenu().add("Dashboard");
            popupMenu.getMenu().add("Routers");
            popupMenu.getMenu().add("Log out");
            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                switch (title) {
                    case "Dashboard":
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                        return true;
                    case "Routers":
                        startActivity(new Intent(this, RouterListActivity.class));
                        finish();
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

    private void startWifiScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Wi-Fi scan requires location permission", Toast.LENGTH_SHORT).show();
            return;
        }

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                scanResults = wifiManager.getScanResults();
                List<String> wifiNames = new ArrayList<>();
                for (ScanResult result : scanResults) {
                    wifiNames.add(result.SSID + " - " + result.BSSID);
                }

                wifiNames.add("DummyRouter001 - 00:11:22:33:44:55");

                adapter = new ArrayAdapter<>(WifiScanActivity.this, android.R.layout.simple_list_item_1, wifiNames);
                wifiListView.setAdapter(adapter);

                wifiListView.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedName = wifiNames.get(position);

                    if (selectedName.startsWith("DummyRouter001")) {
                        showDummyRouterDialog();
                    } else {
                        Toast.makeText(WifiScanActivity.this, "Selected: " + selectedName, Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));

                        new android.os.Handler().postDelayed(() -> {
                            int ip = wifiManager.getConnectionInfo().getIpAddress();
                            String formattedIp = android.text.format.Formatter.formatIpAddress(ip);
                            Toast.makeText(WifiScanActivity.this, "Current IP: " + formattedIp, Toast.LENGTH_LONG).show();
                        }, 4000);
                    }
                });

                try {
                    unregisterReceiver(this);
                } catch (IllegalArgumentException ignored) {}
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        try {
            boolean success = wifiManager.startScan();
            if (!success) {
                Toast.makeText(this, "Failed to start Wi-Fi scan", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error during scan", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDummyRouterDialog() {
        EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle("DummyRouter001")
                .setMessage("Password: admin123\n\nPlease enter the password to connect.\n:")
                .setView(passwordInput)
                .setPositiveButton("Connection", (dialog, which) -> {
                    String input = passwordInput.getText().toString().trim();
                    if (input.equals("admin123")) {
                        Toast.makeText(this, "Connected to DummyRouter001", Toast.LENGTH_SHORT).show();
                        new android.os.Handler().postDelayed(this::showDummyRouterConnectedDialog, 1000);
                    } else {
                        Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDummyRouterConnectedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Connected")
                .setMessage("IP: 192.168.0.1\nUser: admin\nPassword: admin")
                .setPositiveButton("Save Router", (dialog, which) -> {
                    Intent i = new Intent(WifiScanActivity.this, AddRouterActivity.class);
                    i.putExtra("routerIp", "192.168.0.1");
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWifiScan();
            } else {
                Toast.makeText(this, "Permission required to scan Wi-Fi", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
