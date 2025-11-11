package com.example.appslocker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.Constants;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.ui.child.ChildMainActivity;
import com.example.appslocker.ui.child.ScanQRActivity;
import com.example.appslocker.ui.parent.ParentMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelection";
    private AppPreferences appPreferences;
    private FirebaseAuthManager authManager;

    private Button btnParent, btnChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        appPreferences = new AppPreferences(this);
        authManager = FirebaseAuthManager.getInstance();

        initViews();
        setupClickListeners();

        // Проверяем, не выбрана ли уже роль
        checkExistingRole();
    }

    private void initViews() {
        btnParent = findViewById(R.id.btn_parent);
        btnChild = findViewById(R.id.btn_child);
    }

    private void setupClickListeners() {
        btnParent.setOnClickListener(v -> selectRole(Constants.ROLE_PARENT));
        btnChild.setOnClickListener(v -> selectRole(Constants.ROLE_CHILD));
    }

    private void checkExistingRole() {
        String selectedRole = appPreferences.getSelectedRole();
        if (!Constants.ROLE_NONE.equals(selectedRole)) {
            Log.d(TAG, "Role already selected: " + selectedRole + ", navigating directly");
            navigateToRoleActivity(selectedRole);
        }
    }

    private void selectRole(String role) {
        Log.d(TAG, "Role selected: " + role);

        if (Constants.ROLE_CHILD.equals(role)) {
            // Для детского режима обеспечиваем аутентификацию (анонимную или существующую)
            ensureAuthenticationForChild(role);
        } else {
            saveRoleAndNavigate(role);
        }
    }

    private void ensureAuthenticationForChild(String role) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Если пользователь не аутентифицирован, создаем анонимного
        if (currentUser == null) {
            Log.d(TAG, "No user authenticated, creating anonymous user for child role");
            createAnonymousUser(role);
        } else {
            // Пользователь уже аутентифицирован, продолжаем
            Log.d(TAG, "User already authenticated: " + currentUser.getUid() +
                    ", anonymous: " + currentUser.isAnonymous());
            saveRoleAndNavigate(role);
        }
    }

    private void createAnonymousUser(String role) {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Anonymous user created: " + user.getUid() +
                                    ", anonymous: " + user.isAnonymous());
                            // Сохраняем информацию об анонимном пользователе
                            appPreferences.saveUserInfo(user);
                        }
                        saveRoleAndNavigate(role);
                    } else {
                        Toast.makeText(this, "Ошибка анонимного входа: " +
                                        (task.getException() != null ?
                                                task.getException().getMessage() : "Неизвестная ошибка"),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Anonymous sign-in failed", task.getException());
                    }
                });
    }

    private void saveRoleAndNavigate(String role) {
        appPreferences.setSelectedRole(role);
        appPreferences.setFirstLaunch(false);
        navigateToRoleActivity(role);
    }

    private void navigateToRoleActivity(String role) {
        Intent intent;

        if (Constants.ROLE_PARENT.equals(role)) {
            intent = new Intent(this, ParentMainActivity.class);
        } else {
            // Для детского режима проверяем, связано ли устройство
            if (appPreferences.isDeviceLinked()) {
                intent = new Intent(this, ChildMainActivity.class);
            } else {
                intent = new Intent(this, ScanQRActivity.class);
            }
        }

        Log.d(TAG, "Navigating to: " + intent.getComponent().getClassName());
        startActivity(intent);
        finish();
    }
}