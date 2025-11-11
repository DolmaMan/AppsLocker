package com.example.appslocker.ui.parent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.data.repository.LinkRepository;
import com.example.appslocker.ui.RoleSelectionActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;

public class ParentMainActivity extends AppCompatActivity implements FirebaseAuthManager.AuthListener {

    private AppPreferences appPreferences;
    private FirebaseAuthManager authManager;
    private LinkRepository linkRepository;

    private Button btnGenerateQR, btnDashboard, btnSwitchRole, btnSignIn;
    private TextView tvRoleInfo, tvDeviceInfo, tvAuthStatus;
    private View authSection, noAuthSection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_main);

        appPreferences = new AppPreferences(this);
        authManager = FirebaseAuthManager.getInstance();
        linkRepository = new LinkRepository();

        initViews();
        setupClickListeners();

        authManager.initializeGoogleSignIn(this);
        authManager.addAuthListener(this);

        updateUI();
    }

    private void initViews() {
        tvRoleInfo = findViewById(R.id.tv_role_info);
        tvDeviceInfo = findViewById(R.id.tv_device_info);
        tvAuthStatus = findViewById(R.id.tv_auth_status);

        btnGenerateQR = findViewById(R.id.btn_generate_qr);
        btnDashboard = findViewById(R.id.btn_dashboard);
        btnSwitchRole = findViewById(R.id.btn_switch_role);
        btnSignIn = findViewById(R.id.btn_sign_in);

        authSection = findViewById(R.id.auth_section);
        noAuthSection = findViewById(R.id.no_auth_section);
    }

    private void setupClickListeners() {
        btnGenerateQR.setOnClickListener(v -> {
            if (checkGoogleAuthentication()) {
                startGenerateQRActivity();
            }
        });

        btnDashboard.setOnClickListener(v -> {
            if (checkGoogleAuthentication()) {
                startDashboardActivity();
            }
        });

        btnSwitchRole.setOnClickListener(v -> switchToRoleSelection());
        btnSignIn.setOnClickListener(v -> signIn());
    }

    private void updateUI() {
        boolean isGoogleAuthenticated = authManager.isGoogleUser();

        if (isGoogleAuthenticated) {
            // Пользователь авторизован через Google
            String userEmail = authManager.getCurrentUserEmail();
            String userName = authManager.getCurrentUserName();

            tvAuthStatus.setText("Аутентифицирован через Google");
            tvRoleInfo.setText("Режим: Родительский");
            tvDeviceInfo.setText("ID устройства: " + appPreferences.getDeviceId());

            authSection.setVisibility(View.VISIBLE);
            noAuthSection.setVisibility(View.GONE);

            // Обновляем кнопки
            btnSignIn.setText("Сменить аккаунт");

        } else {
            // Пользователь не авторизован через Google
            tvAuthStatus.setText("Требуется вход через Google");
            tvRoleInfo.setText("Режим: Родительский");
            tvDeviceInfo.setText("ID устройства: " + appPreferences.getDeviceId());

            authSection.setVisibility(View.GONE);
            noAuthSection.setVisibility(View.VISIBLE);

            // Обновляем кнопки
            btnSignIn.setText("Войти через Google");
        }
    }

    private boolean checkGoogleAuthentication() {
        if (!authManager.isGoogleUser()) {
            Toast.makeText(this, "Для использования родительских функций требуется вход через Google",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void signIn() {
        // Если уже авторизован, предлагаем сменить аккаунт
        if (authManager.isGoogleUser()) {
            showAccountSwitchDialog();
        } else {
            startGoogleSignIn();
        }
    }

    private void startGoogleSignIn() {
        Intent signInIntent = authManager.getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }

    private void showAccountSwitchDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Сменить аккаунт")
                .setMessage("Вы хотите выйти из текущего аккаунта и войти в другой?")
                .setPositiveButton("Сменить", (dialog, which) -> {
                    authManager.signOut();
                    startGoogleSignIn();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void startGenerateQRActivity() {
        Intent intent = new Intent(this, GenerateQRActivity.class);
        startActivity(intent);
    }

    private void startDashboardActivity() {
        Intent intent = new Intent(this, ParentDashboardActivity.class);
        startActivity(intent);
    }

    private void switchToRoleSelection() {
        appPreferences.setSelectedRole(appPreferences.ROLE_NONE);
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(data);
            authManager.handleGoogleSignInResult(task);
        }
    }

    // Реализация AuthListener - автоматическое обновление UI
    @Override
    public void onAuthSuccess(FirebaseUser user) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Аутентификация успешна!", Toast.LENGTH_SHORT).show();
            appPreferences.saveUserInfo(user);
            updateUI(); // Автоматически обновляем интерфейс
        });
    }

    @Override
    public void onAuthError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Ошибка аутентификации: " + error, Toast.LENGTH_LONG).show();
            updateUI(); // Обновляем UI даже при ошибке
        });
    }

    @Override
    public void onAuthSignedOut() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
            appPreferences.clearUserInfo();
            updateUI(); // Обновляем UI после выхода
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI(); // Обновляем UI при возвращении на экран
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authManager.removeAuthListener(this);
    }
}