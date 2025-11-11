package com.example.appslocker.ui.parent;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.core.FirebaseManager;
import com.example.appslocker.utils.QRCodeGenerator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class GenerateQRActivity extends AppCompatActivity {

    private DatabaseReference notificationsRef;
    private ValueEventListener notificationsListener;
    private boolean isLinkEstablished = false;

    private AppPreferences appPreferences;
    private FirebaseAuthManager authManager;
    private ImageView qrCodeImage;
    private TextView tvDeviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qractivity);

        appPreferences = new AppPreferences(this);
        authManager = FirebaseAuthManager.getInstance();

        // Проверяем аутентификацию
        if (!authManager.isUserSignedIn()) {
            Toast.makeText(this, "Требуется аутентификация", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        generateQRCode();
        setupLinkingNotificationsListener();
    }

    private void setupLinkingNotificationsListener() {
        String parentDeviceId = appPreferences.getDeviceId();
        notificationsRef = FirebaseManager.getInstance().getDatabaseReference("linkingNotifications");

        if (notificationsRef == null) {
            Log.e("GenerateQR", "Failed to get notifications reference");
            return;
        }

        Log.d("GenerateQR", "Setting up notifications listener for: " + parentDeviceId);

        // Слушаем уведомления, где parentDeviceId равен нашему deviceId и read == false
        notificationsListener = notificationsRef.orderByChild("parentDeviceId").equalTo(parentDeviceId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (isLinkEstablished) {
                            return; // Уже обработали соединение
                        }

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            // Проверяем, не прочитано ли уведомление
                            Boolean read = snapshot.child("read").getValue(Boolean.class);
                            String linkId = snapshot.child("linkId").getValue(String.class);
                            String childDeviceId = snapshot.child("childDeviceId").getValue(String.class);

                            if (read != null && !read && linkId != null) {
                                Log.d("GenerateQR", "New linking notification received: " + linkId);

                                // Помечаем уведомление как прочитанное
                                markNotificationAsRead(snapshot.getKey());

                                // Показываем уведомление и закрываем активность
                                handleSuccessfulConnection(linkId, childDeviceId);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("GenerateQR", "Error loading notifications: " + databaseError.getMessage());
                    }
                });
    }

    private void markNotificationAsRead(String notificationId) {
        if (notificationsRef == null) return;

        // УДАЛЯЕМ уведомление вместо пометки как прочитанное
        notificationsRef.child(notificationId).removeValue()
                .addOnSuccessListener(aVoid -> Log.d("GenerateQR", "Notification removed from Firebase"))
                .addOnFailureListener(e -> Log.e("GenerateQR", "Failed to remove notification: " + e.getMessage()));
    }

    private void handleSuccessfulConnection(String linkId, String childDeviceId) {
        isLinkEstablished = true;

        runOnUiThread(() -> {
            // Показываем сообщение об успешном подключении
            androidx.appcompat.app.AlertDialog.Builder builder =
                    new androidx.appcompat.app.AlertDialog.Builder(this);

            builder.setTitle("Устройство подключено!")
                    .setMessage("Дочернее устройство успешно подключено!\n" +
                            "ID связи: " + linkId +
                            (childDeviceId != null ? "\nID дочернего устройства: " + childDeviceId : ""))
                    .setPositiveButton("OK", (dialog, which) -> {
                        navigateToParentMain();
                    })
                    .setCancelable(false)
                    .show();

            // Обновляем информацию на экране
            tvDeviceInfo.setText("✅ Устройство успешно подключено!\n\n" +
                    "Родительское устройство: " + appPreferences.getDeviceName() + "\n" +
                    "ID связи: " + linkId);

            Toast.makeText(this, "Устройство успешно подключено!", Toast.LENGTH_LONG).show();

            // Автоматическое закрытие через 5 секунд, если пользователь не нажал OK
            new Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    navigateToParentMain();
                }
            }, 5000);
        });
    }

    private void initViews() {
        qrCodeImage = findViewById(R.id.qrCodeImage);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);

        String deviceInfo = "Устройство: " + appPreferences.getDeviceName() +
                "\nID: " + appPreferences.getDeviceId();
        tvDeviceInfo.setText(deviceInfo);

        // Настройка обработчика нажатия на стрелку "назад" в тулбаре
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            // Обработка нажатия на стрелку "назад"
            onBackPressed();
        });

    }

    @Override
    public void onBackPressed() {
        navigateToParentMain();
    }

    private void navigateToParentMain() {
        Intent intent = new Intent(this, ParentMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void generateQRCode() {
        String parentDeviceId = appPreferences.getDeviceId();

        // Используем аутентифицированного пользователя
        FirebaseUser user = authManager.getCurrentUser();
        String parentUserId = user != null ? user.getUid() : appPreferences.getUserId();
        String parentEmail = user != null ? user.getEmail() : appPreferences.getUserEmail();

        String qrData = QRCodeGenerator.createLinkData(parentDeviceId);
        if (qrData != null) {
            Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrData, 600, 600);
            if (qrBitmap != null) {
                qrCodeImage.setImageBitmap(qrBitmap);

                // Обновляем информацию об устройстве
                String deviceInfo = "Устройство: " + appPreferences.getDeviceName() +
                        "\nID: " + parentDeviceId +
                        "\nПользователь: " + (user != null ? user.getEmail() : "Не аутентифицирован");
                tvDeviceInfo.setText(deviceInfo);
            } else {
                Toast.makeText(this, "Ошибка генерации QR-кода", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Убираем слушатель при уничтожении активности
        if (notificationsRef != null && notificationsListener != null) {
            notificationsRef.removeEventListener(notificationsListener);
        }
    }
}