package com.example.appslocker.data.repository;

import android.util.Log;

import com.example.appslocker.core.FirebaseAuthManager;
import com.example.appslocker.core.FirebaseManager;
import com.example.appslocker.data.model.DeviceLink;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class LinkRepository {
    private static final String TAG = "LinkRepository";
    private DatabaseReference linksRef;
    private DatabaseReference devicesRef;
    private FirebaseManager firebaseManager;
    private FirebaseAuthManager authManager;

    public LinkRepository() {
        firebaseManager = FirebaseManager.getInstance();
        authManager = FirebaseAuthManager.getInstance();

        if (firebaseManager.isInitialized()) {
            linksRef = firebaseManager.getDatabaseReference("deviceLinks");
            devicesRef = firebaseManager.getDatabaseReference("devices");
        } else {
            Log.e(TAG, "Firebase not initialized in LinkRepository");
        }
    }

    public void removeDeviceLink(String linkId, LinkCallback deviceRemove) {
    }

    public interface LinkCallback {
        void onSuccess(DeviceLink link);
        void onError(String error);
    }

    public interface LinkCheckCallback {
        void onLinked(boolean isLinked, DeviceLink link);
        void onError(String error);
    }

    public interface BlackListCallback {
        void onSuccess();
        void onError(String error);
    }

    // Проверка аутентификации перед операциями
    private boolean checkAuthentication() {
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User not authenticated for database operation");
            return false;
        }
        return true;
    }

    // Создание новой связи
    public void createDeviceLink(DeviceLink link, LinkCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация для создания связи");
            return;
        }

        if (linksRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        FirebaseUser user = authManager.getCurrentUser();
        String linkId = linksRef.push().getKey();
        if (linkId != null) {
            link.setLinkId(linkId);
            link.setActive(true); // Убедитесь, что связь активна

            linksRef.child(linkId).setValue(link)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Device link created successfully: " + linkId);
                        saveDeviceInfo(link.getParentDeviceId(), "parent");
                        saveDeviceInfo(link.getChildDeviceId(), "child");

                        // Отправляем уведомление родителю
                        notifyParentDeviceLinked(link.getParentDeviceId(), link.getChildDeviceId(), linkId,
                                new NotificationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Parent notification sent");
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, "Failed to send parent notification: " + error);
                                    }
                                });

                        callback.onSuccess(link);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create device link: " + e.getMessage());
                        callback.onError("Database error: " + e.getMessage());
                    });
        } else {
            callback.onError("Failed to generate link ID");
        }
    }

    // Проверка связи устройства
    public void checkDeviceLink(String deviceId, LinkCheckCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация для проверки связи");
            return;
        }

        if (linksRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        Log.d(TAG, "Checking device link for: " + deviceId);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        DeviceLink link = snapshot.getValue(DeviceLink.class);
                        if (link != null && link.isActive()) {
                            Log.d(TAG, "Found active link: " + link.getLinkId());
                            callback.onLinked(true, link);
                            return;
                        }
                    }
                }
                callback.onLinked(false, null);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Database error checking link: " + databaseError.getMessage());
                callback.onError("Database error: " + databaseError.getMessage());
            }
        };

        // Проверяем как parent устройство
        linksRef.orderByChild("parentDeviceId").equalTo(deviceId)
                .addListenerForSingleValueEvent(listener);
    }

    // Получение связи по ID
    public void getDeviceLink(String linkId, LinkCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация");
            return;
        }

        if (linksRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        linksRef.child(linkId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DeviceLink link = dataSnapshot.getValue(DeviceLink.class);
                if (link != null && link.isActive()) {
                    callback.onSuccess(link);
                } else {
                    callback.onError("Связь не найдена или неактивна");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Ошибка базы данных: " + databaseError.getMessage());
            }
        });
    }

    // Добавление приложения в черный список
    public void addToBlackList(String linkId, String packageName, BlackListCallback callback) {
        updateBlackList(linkId, packageName, System.currentTimeMillis(), callback);
    }

    // Удаление приложения из черного списка
    public void removeFromBlackList(String linkId, String packageName, BlackListCallback callback) {
        updateBlackList(linkId, packageName, null, callback);
    }

    private void updateBlackList(String linkId, String packageName, Long timestamp, BlackListCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация");
            return;
        }

        if (linksRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        // Сначала получаем текущую связь
        linksRef.child(linkId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DeviceLink link = dataSnapshot.getValue(DeviceLink.class);
                if (link == null) {
                    callback.onError("Связь не найдена");
                    return;
                }

                Map<String, Long> blackListApps = link.getBlackListApps();
                if (blackListApps == null) {
                    blackListApps = new HashMap<>();
                }

                if (timestamp != null) {
                    // Добавляем или обновляем приложение
                    blackListApps.put(packageName, timestamp);
                } else {
                    // Удаляем приложение
                    blackListApps.remove(packageName);
                }

                link.setBlackListApps(blackListApps);

                // Обновляем в базе данных
                linksRef.child(linkId).child("blackListApps").setValue(blackListApps)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Blacklist updated for link: " + linkId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to update blacklist: " + e.getMessage());
                            callback.onError("Ошибка обновления: " + e.getMessage());
                        });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Ошибка базы данных: " + databaseError.getMessage());
            }
        });
    }

    // Получение черного списка для связи
    public void getBlackList(String linkId, BlackListLoadCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация");
            return;
        }

        if (linksRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        linksRef.child(linkId).child("blackListApps").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Long> blackListApps = new HashMap<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String packageName = snapshot.getKey();
                        Long timestamp = snapshot.getValue(Long.class);
                        if (packageName != null && timestamp != null) {
                            blackListApps.put(packageName, timestamp);
                        }
                    }
                }
                callback.onSuccess(blackListApps);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Ошибка базы данных: " + databaseError.getMessage());
            }
        });
    }

    public interface BlackListLoadCallback {
        void onSuccess(Map<String, Long> blackListApps);
        void onError(String error);
    }

    private void saveDeviceInfo(String deviceId, String role) {
        if (devicesRef == null || !checkAuthentication()) return;

        Device device = new Device(deviceId, role, System.currentTimeMillis());
        devicesRef.child(deviceId).setValue(device)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Device info saved: " + deviceId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save device info: " + e.getMessage()));
    }

    // Внутренний класс Device для хранения информации об устройстве
    public static class Device {
        public String deviceId;
        public String role;
        public long registeredAt;

        public Device() {}

        public Device(String deviceId, String role, long registeredAt) {
            this.deviceId = deviceId;
            this.role = role;
            this.registeredAt = registeredAt;
        }
    }

    public interface NotificationCallback {
        void onSuccess();
        void onError(String error);
    }

    // Отправка уведомления родительскому устройству
    public void notifyParentDeviceLinked(String parentDeviceId, String childDeviceId, String linkId, NotificationCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация для отправки уведомления");
            return;
        }

        DatabaseReference notificationsRef = firebaseManager.getDatabaseReference("linkingNotifications");
        if (notificationsRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        // Проверяем, не было ли уже отправлено уведомление для этой связи
        notificationsRef.orderByChild("linkId").equalTo(linkId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Уведомление уже существует, не создаем дубликат
                            Log.d(TAG, "Notification already exists for link: " + linkId);
                            callback.onSuccess();
                            return;
                        }

                        // Создаем новое уведомление
                        createNewNotification(parentDeviceId, childDeviceId, linkId, callback);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error checking existing notifications: " + databaseError.getMessage());
                        // При ошибке проверки все равно создаем уведомление
                        createNewNotification(parentDeviceId, childDeviceId, linkId, callback);
                    }
                });
    }

    private void createNewNotification(String parentDeviceId, String childDeviceId, String linkId, NotificationCallback callback) {
        DatabaseReference notificationsRef = firebaseManager.getDatabaseReference("linkingNotifications");
        String notificationId = notificationsRef.push().getKey();

        if (notificationId == null) {
            callback.onError("Не удалось создать уведомление");
            return;
        }

        // Создаем объект уведомления
        Map<String, Object> notification = new HashMap<>();
        notification.put("parentDeviceId", parentDeviceId);
        notification.put("childDeviceId", childDeviceId);
        notification.put("linkId", linkId);

        notificationsRef.child(notificationId).setValue(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Parent notification sent successfully: " + notificationId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send parent notification: " + e.getMessage());
                    callback.onError("Ошибка отправки уведомления: " + e.getMessage());
                });
    }

    private String getDeviceName() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }

    public void getChildDeviceInfo(String childDeviceId, DeviceInfoCallback callback) {
        if (!checkAuthentication()) {
            callback.onError("Требуется аутентификация");
            return;
        }

        DatabaseReference devicesRef = firebaseManager.getDatabaseReference("devices");
        if (devicesRef == null) {
            callback.onError("Firebase not initialized");
            return;
        }

        devicesRef.child(childDeviceId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String deviceName = dataSnapshot.child("deviceName").getValue(String.class);
                    String role = dataSnapshot.child("role").getValue(String.class);

                    Map<String, Object> deviceInfo = new HashMap<>();
                    deviceInfo.put("deviceName", deviceName != null ? deviceName : "Детское устройство");
                    deviceInfo.put("role", role);
                    deviceInfo.put("deviceId", childDeviceId);

                    callback.onSuccess(deviceInfo);
                } else {
                    callback.onError("Устройство не найдено");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError("Ошибка базы данных: " + databaseError.getMessage());
            }
        });
    }

    public interface DeviceInfoCallback {
        void onSuccess(Map<String, Object> deviceInfo);
        void onError(String error);
    }
}