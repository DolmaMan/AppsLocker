package com.example.appslocker.ui.parent;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appslocker.R;
import com.example.appslocker.core.AppPreferences;
import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.core.FirebaseManager;
import com.example.appslocker.data.model.ChildDevice;
import com.example.appslocker.data.model.DeviceLink;
import com.example.appslocker.data.repository.LinkRepository;
import com.example.appslocker.ui.RoleSelectionActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParentDashboardActivity extends AppCompatActivity implements
        FirebaseAuthManager.AuthListener,
        ChildDevicesAdapter.OnChildDeviceClickListener {

    private static final String TAG = "ParentDashboard";

    private AppPreferences appPreferences;
    private FirebaseAuthManager authManager;
    private LinkRepository linkRepository;

    private TextView tvConnectedDevices, tvActiveRestrictions;
    private RecyclerView rvLinkedDevices;
    private Button btnAddDevice, btnManageRestrictions, btnViewReports, btnAppManagement, btnTimeLimits, btnSettings, btnSwitchRole;

    private ChildDevicesAdapter childDevicesAdapter;
    private List<ChildDevice> childDevices = new ArrayList<>();

    private DatabaseReference linksRef;
    private ValueEventListener linksListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        appPreferences = new AppPreferences(this);
        authManager = FirebaseAuthManager.getInstance();
        linkRepository = new LinkRepository();

        initViews();
        setupRecyclerView();
        setupClickListeners();

        authManager.initializeGoogleSignIn(this);
        authManager.addAuthListener(this); // Регистрируем слушатель

        updateUserInfo();
        loadChildDevices();
    }

    private void initViews() {
        tvConnectedDevices = findViewById(R.id.tv_connected_devices);
        tvActiveRestrictions = findViewById(R.id.tv_active_restrictions);

        rvLinkedDevices = findViewById(R.id.rv_linked_devices);

        btnAddDevice = findViewById(R.id.btn_add_device);
        btnManageRestrictions = findViewById(R.id.btn_manage_restrictions);
        btnViewReports = findViewById(R.id.btn_view_reports);
        btnAppManagement = findViewById(R.id.btn_app_management);
        btnTimeLimits = findViewById(R.id.btn_time_limits);
        btnSettings = findViewById(R.id.btn_settings);
        btnSwitchRole = findViewById(R.id.btn_switch_role_d);

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

    private void setupRecyclerView() {
        childDevicesAdapter = new ChildDevicesAdapter(childDevices, this);
        rvLinkedDevices.setLayoutManager(new LinearLayoutManager(this));
        rvLinkedDevices.setAdapter(childDevicesAdapter);

        // Устанавливаем видимость RecyclerView
        rvLinkedDevices.setVisibility(childDevices.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void setupClickListeners() {
        btnAddDevice.setOnClickListener(v -> addNewDevice());
        btnManageRestrictions.setOnClickListener(v -> manageRestrictions());
        btnViewReports.setOnClickListener(v -> viewReports());
        btnAppManagement.setOnClickListener(v -> manageApps());
        btnTimeLimits.setOnClickListener(v -> setTimeLimits());
        btnSettings.setOnClickListener(v -> openSettings());
        btnSwitchRole.setOnClickListener(v -> switchToRoleSelection());
    }

    private void loadChildDevices() {
        String parentDeviceId = appPreferences.getDeviceId();
        linksRef = FirebaseManager.getInstance().getDatabaseReference("deviceLinks");

        if (linksRef == null) {
            Log.e(TAG, "Failed to get deviceLinks reference");
            showEmptyState();
            return;
        }

        Log.d(TAG, "Loading child devices for parent: " + parentDeviceId);

        // Удаляем старый слушатель если есть
        if (linksListener != null) {
            linksRef.removeEventListener(linksListener);
        }

        // Слушаем ВСЕ deviceLinks и фильтруем по parentDeviceId
        linksListener = linksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange: total links in database: " + dataSnapshot.getChildrenCount());

                List<ChildDevice> foundChildDevices = new ArrayList<>();
                int totalBlockedApps = 0;

                // Перебираем ВСЕ связи в базе
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        DeviceLink link = snapshot.getValue(DeviceLink.class);

                        if (link != null &&
                                link.isActive() &&
                                parentDeviceId.equals(link.getParentDeviceId())) {

                            Log.d(TAG, "Found child device: " + link.getChildDeviceId() + " for parent: " + parentDeviceId);

                            // Создаем ChildDevice из DeviceLink
                            ChildDevice childDevice = createChildDeviceFromLink(link);
                            foundChildDevices.add(childDevice);

                            // Считаем заблокированные приложения
                            if (childDevice.getBlockedApps() != null) {
                                totalBlockedApps += childDevice.getBlockedApps().size();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing device link: " + e.getMessage());
                    }
                }

                Log.d(TAG, "Total child devices found: " + foundChildDevices.size());

                // ОБНОВЛЯЕМ RecyclerView
                updateChildDevicesList(foundChildDevices, totalBlockedApps);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading device links: " + databaseError.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ParentDashboardActivity.this,
                            "Ошибка загрузки устройств",
                            Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    private ChildDevice createChildDeviceFromLink(DeviceLink link) {
        // Создаем базовый ChildDevice из DeviceLink
        ChildDevice childDevice = new ChildDevice(
                link.getChildDeviceId(),
                "Детское устройство", // Временное имя
                link.getLinkId()
        );

        // Копируем черный список
        if (link.getBlackListApps() != null) {
            childDevice.setBlockedApps(new HashMap<>(link.getBlackListApps()));
        }

        // Устанавливаем статус онлайн
        childDevice.setOnline(true);

        return childDevice;
    }

    private void updateChildDevicesList(List<ChildDevice> newChildDevices, int totalBlockedApps) {
        runOnUiThread(() -> {
            // Очищаем и обновляем основной список
            childDevices.clear();
            childDevices.addAll(newChildDevices);

            // Обновляем адаптер
            childDevicesAdapter.updateDevices(childDevices);

            if (childDevices.isEmpty()) {
                rvLinkedDevices.setVisibility(View.GONE);
            } else {
                rvLinkedDevices.setVisibility(View.VISIBLE);
            }

            // Обновляем статистику
            updateStatistics(childDevices.size(), totalBlockedApps);

            Log.d(TAG, "RecyclerView updated with " + childDevices.size() + " devices");
        });
    }

    private void showEmptyState() {
        runOnUiThread(() -> {
            childDevices.clear();
            childDevicesAdapter.updateDevices(childDevices);
            rvLinkedDevices.setVisibility(View.GONE);
            updateStatistics(0, 0);
        });
    }

    private void updateStatistics(int devicesCount, int blockedAppsCount) {
        tvConnectedDevices.setText(String.valueOf(devicesCount));
        tvActiveRestrictions.setText(String.valueOf(blockedAppsCount));
    }

    private void updateUserInfo() {
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_parent_dashboard, menu);
        updateMenuItems(menu);
        return true;
    }

    private void updateMenuItems(Menu menu) {
        MenuItem signInItem = menu.findItem(R.id.action_sign_in);
        MenuItem signOutItem = menu.findItem(R.id.action_sign_out);
        MenuItem accountItem = menu.findItem(R.id.action_account);

        if (authManager.isGoogleUser()) {
            // Пользователь авторизован через Google
            signInItem.setVisible(false);
            signOutItem.setVisible(true);
            accountItem.setVisible(true);

            // Обновляем заголовок аккаунта
            String userEmail = authManager.getCurrentUserEmail();
            accountItem.setTitle(userEmail != null ? userEmail : "Аккаунт");
        } else {
            // Пользователь не авторизован
            signInItem.setVisible(true);
            signOutItem.setVisible(false);
            accountItem.setVisible(false);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuItems(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sign_in) {
            signIn();
            return true;
        } else if (id == R.id.action_sign_out) {
            signOut();
            return true;
        } else if (id == R.id.action_account) {
            showAccountInfo();
            return true;
        } else if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_refresh) {
            loadChildDevices();
            Toast.makeText(this, "Обновление данных...", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void signIn() {
        Intent signInIntent = authManager.getGoogleSignInClient().getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }

    private void signOut() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти из аккаунта?")
                .setPositiveButton("Выйти", (dialog, which) -> {
                    authManager.signOut();
                    appPreferences.clearUserInfo();
                    updateUserInfo();
                    invalidateOptionsMenu();
                    Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
                    onBackPressed();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showAccountInfo() {
        FirebaseUser user = authManager.getCurrentUser();
        String message;

        if (user != null) {
            message = "Аккаунт: " + user.getEmail() +
                    "\nID: " + user.getUid() +
                    "\nИмя: " + (user.getDisplayName() != null ? user.getDisplayName() : "Не указано");
        } else {
            message = "Информация об аккаунте недоступна";
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Информация об аккаунте")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
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

    // Реализация методов интерфейса ChildDevicesAdapter.OnChildDeviceClickListener
    @Override
    public void onDeviceClick(ChildDevice device) {
        showDeviceDetails(device);
    }

    @Override
    public void onManageClick(ChildDevice device) {
        manageChildDevice(device);
    }

    private void showDeviceDetails(ChildDevice device) {
        StringBuilder blockedAppsInfo = new StringBuilder();
        if (device.getBlockedApps() != null && !device.getBlockedApps().isEmpty()) {
            blockedAppsInfo.append("\nЗаблокированные приложения:\n");
            for (Map.Entry<String, Long> entry : device.getBlockedApps().entrySet()) {
                blockedAppsInfo.append("• ").append(entry.getKey()).append("\n");
            }
        } else {
            blockedAppsInfo.append("\nНет заблокированных приложений");
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Информация об устройстве")
                .setMessage("Устройство: " + device.getDeviceName() + "\n" +
                        "ID: " + device.getDeviceId() + "\n" +
                        "ID связи: " + device.getLinkId() + "\n" +
                        "Статус: " + (device.isOnline() ? "В сети" : "Не в сети") + "\n" +
                        "Заблокировано приложений: " + device.getBlockedAppsCount() +
                        blockedAppsInfo.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void manageChildDevice(ChildDevice device) {
        Toast.makeText(this, "Управление устройством: " + device.getDeviceName(),
                Toast.LENGTH_SHORT).show();
        // TODO: Реализовать управление устройством
    }

    private void removeChildDevice(ChildDevice device) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Удаление устройства")
                .setMessage("Вы уверены, что хотите удалить устройство \"" +
                        device.getDeviceName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    // Удаляем устройство из Firebase
                    linkRepository.removeDeviceLink(device.getLinkId(), new LinkRepository.LinkCallback() {
                        @Override
                        public void onSuccess(DeviceLink link) {
                            Toast.makeText(ParentDashboardActivity.this,
                                    "Устройство удалено", Toast.LENGTH_SHORT).show();
                            // Данные автоматически обновятся через слушатель Firebase
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(ParentDashboardActivity.this,
                                    "Ошибка удаления: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // Методы для кнопок
    private void addNewDevice() {
        startActivity(new Intent(this, GenerateQRActivity.class));
    }

    private void manageRestrictions() {
        Toast.makeText(this, "Управление ограничениями", Toast.LENGTH_SHORT).show();
    }

    private void viewReports() {
        Toast.makeText(this, "Просмотр отчетов", Toast.LENGTH_SHORT).show();
    }

    private void manageApps() {
        Toast.makeText(this, "Управление приложениями", Toast.LENGTH_SHORT).show();
    }

    private void setTimeLimits() {
        Toast.makeText(this, "Лимиты времени", Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        Toast.makeText(this, "Настройки", Toast.LENGTH_SHORT).show();
    }

    private void switchToRoleSelection() {
        appPreferences.setSelectedRole(appPreferences.ROLE_NONE);
        Intent intent = new Intent(this, RoleSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, ParentMainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserInfo();
        invalidateOptionsMenu();

        // Перезагружаем данные при возврате на экран
        loadChildDevices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        authManager.removeAuthListener(this);
        // Убираем слушатель при уничтожении активности
        if (linksRef != null && linksListener != null) {
            linksRef.removeEventListener(linksListener);
        }
    }

    @Override
    public void onAuthSuccess(FirebaseUser user) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Аутентификация успешна!", Toast.LENGTH_SHORT).show();
            updateUserInfo();
            invalidateOptionsMenu(); // Принудительно обновляем меню
            loadChildDevices(); // Перезагружаем данные
        });
    }

    @Override
    public void onAuthError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Ошибка аутентификации: " + error, Toast.LENGTH_LONG).show();
            updateUserInfo();
            invalidateOptionsMenu();
        });
    }

    @Override
    public void onAuthSignedOut() {
        runOnUiThread(() -> {
            updateUserInfo();
            invalidateOptionsMenu();
        });
    }
}