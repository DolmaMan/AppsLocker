package com.example.appslocker.ui.child;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.ui.RoleSelectionActivity;

public class ChildMainActivity extends AppCompatActivity {

    private AppPreferences appPreferences;
    private Button btnSwitchRole, btnScanQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_main);

        appPreferences = new AppPreferences(this);
        initViews();
        setupUI();
    }

    private void initViews() {
        btnSwitchRole = findViewById(R.id.btn_switch_role_child);
        btnScanQr = findViewById(R.id.btn_scan_qr);

        btnSwitchRole.setOnClickListener(v -> switchToRoleSelection());
        btnScanQr.setOnClickListener(v -> startScanQr());
    }

    private void setupUI() {

    }

    private void switchToRoleSelection() {
        appPreferences.setSelectedRole(AppPreferences.ROLE_NONE);
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    private void startScanQr() {
        Intent intent = new Intent(this, ScanQRActivity.class);
        startActivity(intent);
        finish();
    }
}