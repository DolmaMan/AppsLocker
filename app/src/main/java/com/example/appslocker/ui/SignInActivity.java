package com.example.appslocker.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.Constants;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.ui.child.ChildMainActivity;
import com.example.appslocker.ui.child.ScanQRActivity;
import com.example.appslocker.ui.parent.ParentMainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity implements FirebaseAuthManager.AuthListener {

    private static final String TAG = "SignInActivity";
    private FirebaseAuthManager authManager;
    private AppPreferences appPreferences;

    private SignInButton btnGoogleSignIn;
    private Button btnSkipAuth;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        authManager = FirebaseAuthManager.getInstance();
        appPreferences = new AppPreferences(this);

        initViews();
        setupClickListeners();

        authManager.initializeGoogleSignIn(this);
        authManager.addAuthListener(this);

        checkExistingAuth();
    }

    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        btnSkipAuth = findViewById(R.id.btn_skip_auth);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tv_status);

        btnGoogleSignIn.setSize(SignInButton.SIZE_WIDE);
    }

    private void setupClickListeners() {
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        btnSkipAuth.setOnClickListener(v -> skipAuthentication());
    }

    private void checkExistingAuth() {
        String selectedRole = appPreferences.getSelectedRole();

        // Если роль уже выбрана и пользователь авторизован соответствующим образом,
        // переходим сразу к нужной активности
        if (!Constants.ROLE_NONE.equals(selectedRole)) {
            if (Constants.ROLE_PARENT.equals(selectedRole) && authManager.isGoogleUser()) {
                Log.d(TAG, "Parent role with Google auth, proceeding to ParentMainActivity");
                proceedToRoleSpecificActivity();
                return;
            } else if (Constants.ROLE_CHILD.equals(selectedRole) && authManager.isUserSignedIn()) {
                Log.d(TAG, "Child role with auth, proceeding to child flow");
                proceedToRoleSpecificActivity();
                return;
            }
        }

        // Показываем экран входа для всех остальных случаев
        tvStatus.setText("Войдите или пропустите аутентификацию");
    }

    private void signInWithGoogle() {
        tvStatus.setText("Запуск Google аутентификации...");
        progressBar.setVisibility(View.VISIBLE);

        Intent signInIntent = authManager.getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }

    private void skipAuthentication() {
        Toast.makeText(this, "Аутентификация пропущена. Некоторые функции могут быть недоступны.",
                Toast.LENGTH_LONG).show();

        // Создаем анонимного пользователя
        authManager.signInAnonymously(new FirebaseAuthManager.AuthListener() {
            @Override
            public void onAuthSuccess(FirebaseUser user) {
                appPreferences.setAuthenticationSkipped(true);
                proceedToRoleSelection();
            }

            @Override
            public void onAuthError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SignInActivity.this,
                            "Ошибка анонимного входа: " + error, Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    tvStatus.setText("Ошибка аутентификации");
                });
            }

            @Override
            public void onAuthSignedOut() {
                // Не используется здесь
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            progressBar.setVisibility(View.VISIBLE);
            tvStatus.setText("Завершение аутентификации...");

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            authManager.handleGoogleSignInResult(task);
        }
    }

    @Override
    public void onAuthSuccess(FirebaseUser user) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Аутентификация успешна!");

            appPreferences.saveUserInfo(user);
            appPreferences.setAuthenticationSkipped(false);

            // После успешной авторизации переходим к соответствующей активности
            proceedToRoleSpecificActivity();
        });
    }

    @Override
    public void onAuthError(String error) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Ошибка аутентификации");
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onAuthSignedOut() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            tvStatus.setText("Вы вышли из системы");
            appPreferences.clearUserInfo();
        });
    }

    private void proceedToRoleSpecificActivity() {
        String selectedRole = appPreferences.getSelectedRole();
        Log.d(TAG, "Proceeding to role-specific activity: " + selectedRole);

        Intent intent;

        if (Constants.ROLE_PARENT.equals(selectedRole)) {
            // Для родителя переходим сразу в главную активность
            intent = new Intent(this, ParentMainActivity.class);
        } else if (Constants.ROLE_CHILD.equals(selectedRole)) {
            // Для ребенка проверяем связь с родительским устройством
            if (appPreferences.isDeviceLinked()) {
                intent = new Intent(this, ChildMainActivity.class);
            } else {
                intent = new Intent(this, ScanQRActivity.class);
            }
        } else {
            // Роль не выбрана, переходим к выбору роли
            intent = new Intent(this, RoleSelectionActivity.class);
        }

        startActivity(intent);
        finish();
    }

    private void proceedToRoleSelection() {
        runOnUiThread(() -> {
            Intent intent = new Intent(SignInActivity.this, RoleSelectionActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authManager.removeAuthListener(this);
    }
}