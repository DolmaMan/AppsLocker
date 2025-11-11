package com.example.appslocker.ui.child;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.core.FirebaseManager;
import com.example.appslocker.data.model.DeviceLink;
import com.example.appslocker.data.repository.LinkRepository;
import com.example.appslocker.ui.RoleSelectionActivity;
import com.example.appslocker.utils.QRCodeScanner;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ScanQRActivity extends AppCompatActivity implements QRCodeScanner.QRScanListener {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private AppPreferences appPreferences;
    private LinkRepository linkRepository;
    private PreviewView cameraPreview;
    private TextView tvScanStatus;
    private ProgressBar progressBar;
    private QRCodeScanner qrCodeScanner;

    private boolean isPermissionGranted = false;
    private boolean isProcessing = false;
    private androidx.appcompat.app.AlertDialog exitDialog; // Добавляем ссылку на диалог
    private String lastProcessedQR = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qractivity);
        initializeFirebase();

        appPreferences = new AppPreferences(this);
        linkRepository = new LinkRepository();
        initViews();

        // Проверяем разрешения перед запуском сканера
        checkCameraPermission();
    }

    private void resetProcessing() {
        isProcessing = false;
    }

    private void initializeFirebase() {
        FirebaseManager firebaseManager = FirebaseManager.getInstance();
        if (!firebaseManager.isInitialized()) {
            firebaseManager.initialize(this);
            Log.d("ScanQR", "FirebaseManager initialized in ScanQRActivity");
        }

        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();
        authManager.initializeGoogleSignIn(this);

        // Проверяем текущего пользователя
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            Log.d("ScanQR", "Current user: " + currentUser.getUid() + ", anonymous: " + currentUser.isAnonymous());
        } else {
            Log.w("ScanQR", "No current user found");
        }
    }

    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        progressBar = findViewById(R.id.progressBar);

        tvScanStatus.setText("Проверка разрешений...");
        qrCodeScanner = new QRCodeScanner();

        // Настройка обработчика нажатия на стрелку "назад" в тулбаре
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            onBackPressed();
        });
    }

    @Override
    public void onBackPressed() {
        if (qrCodeScanner != null) {
            qrCodeScanner.stopScanning();
        }
        returnToRoleSelection();
    }

    private void returnToRoleSelection() {
        // Очищаем выбранную роль чтобы вернуться к выбору
        appPreferences.setSelectedRole(appPreferences.ROLE_NONE);

        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void checkCameraPermission() {
        // Проверяем, есть ли разрешение на камеру
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            isPermissionGranted = true;
            startQRScanner();
        } else {
            // Запрашиваем разрешение
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        tvScanStatus.setText("Запрос разрешения на использование камеры...");

        // Проверяем, нужно ли показывать объяснение
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
            // Показываем объяснение пользователю
            showPermissionExplanation();
        } else {
            // Запрашиваем разрешение напрямую
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void showPermissionExplanation() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Требуется доступ к камере")
                .setMessage("Для сканирования QR-кода необходимо разрешение на использование камеры. " +
                        "Разрешение будет использоваться только для сканирования кодов и не будет " +
                        "сохранять или передавать изображения.")
                .setPositiveButton("Разрешить", (dialog, which) -> {
                    // Запрашиваем разрешение после объяснения
                    ActivityCompat.requestPermissions(
                            ScanQRActivity.this,
                            new String[]{CAMERA_PERMISSION},
                            CAMERA_PERMISSION_REQUEST_CODE
                    );
                })
                .setNegativeButton("Отмена", (dialog, which) -> {
                    // Пользователь отказался, возвращаемся к выбору роли
                    returnToRoleSelection();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено
                isPermissionGranted = true;
                tvScanStatus.setText("Запуск камеры...");
                startQRScanner();
            } else {
                // Разрешение не получено
                isPermissionGranted = false;
                handlePermissionDenied();
            }
        }
    }

    private void handlePermissionDenied() {
        tvScanStatus.setText("Доступ к камере запрещен");

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Доступ к камере запрещен")
                .setMessage("Без доступа к камере невозможно сканировать QR-коды. " +
                        "Вы можете разрешить доступ в настройках приложения или вернуться к выбору роли.")
                .setPositiveButton("Настройки", (dialog, which) -> {
                    // Открываем настройки приложения
                    openAppSettings();
                })
                .setNeutralButton("Выбор роли", (dialog, which) -> {
                    // Возвращаемся к выбору роли
                    returnToRoleSelection();
                })
                .setNegativeButton("Повторить", (dialog, which) -> {
                    // Повторно запрашиваем разрешение
                    checkCameraPermission();
                })
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);

            // Ждем возврата из настроек и проверяем разрешение снова
            new Handler().postDelayed(() -> {
                checkCameraPermission();
            }, 1000);

        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
            // При ошибке предлагаем вернуться к выбору роли
            returnToRoleSelection();
        }
    }

    private void startQRScanner() {
        if (!isPermissionGranted) {
            tvScanStatus.setText("Ожидание разрешения...");
            return;
        }

        try {
            tvScanStatus.setText("Наведите камеру на QR-код");
            qrCodeScanner.startScanning(this, cameraPreview, this, this);
        } catch (Exception e) {
            tvScanStatus.setText("Ошибка запуска камеры");
            Toast.makeText(this, "Ошибка запуска сканера: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();

            // Пытаемся перезапустить через 2 секунды
            new Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    startQRScanner();
                }
            }, 2000);
        }
    }

    @Override
    public void onQRCodeScanned(String result) {
        runOnUiThread(() -> {
            // Защита от повторной обработки того же QR-кода
            if (isProcessing) {
                Log.d("ScanQR", "Already processing QR code, ignoring");
                return;
            }

            if (result.equals(lastProcessedQR)) {
                Log.d("ScanQR", "Duplicate QR code detected, ignoring");
                return;
            }

            isProcessing = true;
            lastProcessedQR = result;

            tvScanStatus.setText("QR-код распознан...");
            progressBar.setVisibility(View.VISIBLE);
            processScannedData(result);
        });
    }

    @Override
    public void onScanError(String error) {
        runOnUiThread(() -> {
            tvScanStatus.setText("Ошибка: " + error);
            Toast.makeText(this, "Ошибка сканирования: " + error, Toast.LENGTH_LONG).show();

            new Handler().postDelayed(() -> {
                if (!isFinishing() && isPermissionGranted) {
                    startQRScanner();
                }
            }, 3000);
        });
    }

    private void linkWithParent(String parentDeviceId) {
        // Проверяем аутентификацию перед созданием связи
        FirebaseAuthManager authManager = FirebaseAuthManager.getInstance();
        if (!authManager.isUserSignedIn()) {
            Toast.makeText(this, "Требуется аутентификация для подключения", Toast.LENGTH_SHORT).show();
            return;
        }

        String childDeviceId = appPreferences.getDeviceId();
        String childDeviceName = appPreferences.getDeviceName();

        // Создаем связь с новой структурой данных
        DeviceLink link = new DeviceLink(parentDeviceId, childDeviceId);

        // Сохраняем информацию об устройстве перед созданием связи
        saveChildDeviceInfo(childDeviceId, childDeviceName);

        linkRepository.createDeviceLink(link, new LinkRepository.LinkCallback() {
            @Override
            public void onSuccess(DeviceLink createdLink) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    appPreferences.setDeviceLinked(true);
                    appPreferences.setLinkedWith(parentDeviceId);
                    appPreferences.setLinkId(createdLink.getLinkId());

                    tvScanStatus.setText("Устройство успешно связано!");
                    Toast.makeText(ScanQRActivity.this,
                            "Устройство связано с родительским", Toast.LENGTH_LONG).show();

                    new Handler().postDelayed(() -> {
                        startActivity(new Intent(ScanQRActivity.this, ChildMainActivity.class));
                        finish();
                    }, 2000);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvScanStatus.setText("Ошибка подключения");
                    Toast.makeText(ScanQRActivity.this,
                            "Ошибка подключения: " + error, Toast.LENGTH_LONG).show();

                    // Перезапускаем сканирование через 3 секунды
                    new Handler().postDelayed(() -> {
                        if (!isFinishing()) {
                            startQRScanner();
                        }
                    }, 3000);
                });
            }
        });
    }

    private void saveChildDeviceInfo(String deviceId, String deviceName) {
        DatabaseReference devicesRef = FirebaseManager.getInstance().getDatabaseReference("devices");
        if (devicesRef == null) return;

        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("deviceName", deviceName);
        deviceInfo.put("role", "child");
        deviceInfo.put("registeredAt", System.currentTimeMillis());

        devicesRef.child(deviceId).setValue(deviceInfo)
                .addOnSuccessListener(aVoid -> Log.d("ScanQR", "Child device info saved"))
                .addOnFailureListener(e -> Log.e("ScanQR", "Failed to save child device info"));
    }

    private void processScannedData(String qrData) {
        try {
            JSONObject json = new JSONObject(qrData);
            String parentDeviceId = json.getString("parentDeviceId");

            linkWithParent(parentDeviceId);
        } catch (JSONException e) {
            // Обработка старого формата QR-кода
            linkWithParent(qrData);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isProcessing = false;
        if (qrCodeScanner != null) {
            qrCodeScanner.stopScanning();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetProcessing();
        // При возврате на активность проверяем разрешение и перезапускаем сканер
        if (isPermissionGranted && qrCodeScanner != null && !qrCodeScanner.isScanning()) {
            new Handler().postDelayed(() -> {
                if (!isFinishing()) {
                    startQRScanner();
                }
            }, 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Закрываем диалог, если активность уничтожается
        if (exitDialog != null && exitDialog.isShowing()) {
            exitDialog.dismiss();
            exitDialog = null;
        }

        if (qrCodeScanner != null) {
            qrCodeScanner.stopScanning();
        }
    }
}