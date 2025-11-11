package com.example.appslocker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.Constants;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.ui.SignInActivity;
import com.example.appslocker.ui.RoleSelectionActivity;
import com.example.appslocker.ui.child.ChildMainActivity;
import com.example.appslocker.ui.child.ScanQRActivity;
import com.example.appslocker.ui.parent.ParentMainActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AppPreferences appPreferences;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        appPreferences = new AppPreferences(this);
        authManager = FirebaseAuthManager.getInstance();

        checkExistingRoleAndNavigate();
    }

    private void checkExistingRoleAndNavigate() {
        String selectedRole = appPreferences.getSelectedRole();
        Log.d(TAG, "Checking existing role: " + selectedRole);

        // Если роль уже выбрана, переходим сразу к соответствующей активности
        if (!Constants.ROLE_NONE.equals(selectedRole)) {
            navigateToRoleSpecificActivity(selectedRole);
        } else {
            // Роль не выбрана, переходим к экрану входа
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        }
    }

    private void navigateToRoleSpecificActivity(String role) {
        Intent intent;

        if (Constants.ROLE_PARENT.equals(role)) {
            // Для родительского режима проверяем Google-авторизацию
            if (authManager.isGoogleUser()) {
                intent = new Intent(this, ParentMainActivity.class);
            } else {
                // Если не авторизован через Google, переходим к входу
                Log.d(TAG, "Parent role selected but no Google auth, going to SignIn");
                intent = new Intent(this, SignInActivity.class);
            }
        } else if (Constants.ROLE_CHILD.equals(role)) {
            // Для детского режима проверяем связь с родительским устройством
            if (appPreferences.isDeviceLinked()) {
                intent = new Intent(this, ChildMainActivity.class);
            } else {
                intent = new Intent(this, ScanQRActivity.class);
            }
        } else {
            // Неизвестная роль, переходим к выбору роли
            intent = new Intent(this, RoleSelectionActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}