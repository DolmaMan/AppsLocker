package com.example.appslocker.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.auth.FirebaseUser;

import java.util.Random;
import java.util.UUID;

public class AppPreferences {
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_AUTH_SKIPPED = "auth_skipped";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public static final String KEY_SELECTED_ROLE = "selected_role";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_IS_LINKED = "is_linked";
    public static final String KEY_LINKED_WITH = "linked_with";
    public static final String KEY_LINK_ID = "link_id";
    public static final String KEY_DEVICE_NAME = "device_name";

    public static final String ROLE_NONE = "none";
    public static final String ROLE_PARENT = "parent";
    public static final String ROLE_CHILD = "child";

    public static final String KEY_USER_AUTHENTICATED = "user_authenticated";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_USER_GOOGLE_ID = "user_google_id";
    public static final String KEY_AUTH_PROVIDER = "auth_provider";

    public AppPreferences(Context context) {
        sharedPref = context.getSharedPreferences("family_app", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
    }

    public boolean isFirstLaunch() {
        return sharedPref.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean firstLaunch) {
        sharedPref.edit().putBoolean(KEY_FIRST_LAUNCH, firstLaunch).apply();
    }

    // Основные методы
    public String getSelectedRole() {
        return sharedPref.getString(KEY_SELECTED_ROLE, ROLE_NONE);
    }

    public void setSelectedRole(String role) {
        editor.putString(KEY_SELECTED_ROLE, role);
        editor.apply();
    }

    public String getDeviceId() {
        String deviceId = sharedPref.getString(KEY_DEVICE_ID, "");
        if (deviceId.isEmpty()) {
            deviceId = generateDeviceId();
            setDeviceId(deviceId);
        }
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.apply();
    }

    public boolean isDeviceLinked() {
        return sharedPref.getBoolean(KEY_IS_LINKED, false);
    }

    public void setDeviceLinked(boolean linked) {
        editor.putBoolean(KEY_IS_LINKED, linked);
        editor.apply();
    }

    public String getLinkedWith() {
        return sharedPref.getString(KEY_LINKED_WITH, "");
    }

    public void setLinkedWith(String deviceId) {
        editor.putString(KEY_LINKED_WITH, deviceId);
        editor.apply();
    }

    public String getLinkId() {
        return sharedPref.getString(KEY_LINK_ID, "");
    }

    public void setLinkId(String linkId) {
        editor.putString(KEY_LINK_ID, linkId);
        editor.apply();
    }

    public String getDeviceName() {
        return sharedPref.getString(KEY_DEVICE_NAME,
                Build.MANUFACTURER + " " + Build.MODEL);
    }

    public void setDeviceName(String deviceName) {
        editor.putString(KEY_DEVICE_NAME, deviceName);
        editor.apply();
    }

    public String getUserId() {
        String userId = sharedPref.getString(KEY_USER_ID, "");
        if (userId.isEmpty()) {
            userId = generateUserId();
            setUserId(userId);
        }
        return userId;
    }

    public void setUserId(String userId) {
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    private String generateDeviceId() {
        return UUID.randomUUID().toString();
    }

    private String generateUserId() {
        return "user_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }

    public void clearLinkInfo() {
        editor.remove(KEY_IS_LINKED);
        editor.remove(KEY_LINKED_WITH);
        editor.remove(KEY_LINK_ID);
        editor.apply();
    }

    public void clearAll() {
        editor.clear();
        editor.apply();
    }

    public void clearPreferences() {
        editor.putString(KEY_SELECTED_ROLE, ROLE_NONE);
        editor.apply();
    }

    public boolean isUserAuthenticated() {
        return (sharedPref.contains(KEY_USER_EMAIL) && !sharedPref.getString(KEY_USER_EMAIL, "").isEmpty() &&
                        sharedPref.contains(KEY_USER_ID));
    }

    public void setUserAuthenticated(boolean authenticated) {
        editor.putBoolean(KEY_USER_AUTHENTICATED, authenticated);
        editor.apply();
    }

    public String getUserEmail() {
        return sharedPref.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public String getUserName() {
        return sharedPref.getString(KEY_USER_NAME, "");
    }

    public void setUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public String getUserGoogleId() {
        return sharedPref.getString(KEY_USER_GOOGLE_ID, "");
    }

    public void setUserGoogleId(String userId) {
        editor.putString(KEY_USER_GOOGLE_ID, userId);
        editor.apply();
    }

    public String getAuthProvider() {
        return sharedPref.getString(KEY_AUTH_PROVIDER, "");
    }

    public void setAuthProvider(String provider) {
        editor.putString(KEY_AUTH_PROVIDER, provider);
        editor.apply();
    }

    public void saveUserInfo(FirebaseUser user) {
        if (user != null) {
            setUserAuthenticated(true);
            setUserId(user.getUid());
            setUserEmail(user.getEmail());
            setUserName(user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
            setAuthProvider(user.getProviderId());
        }
    }

    public boolean isAuthenticationSkipped() {
        return sharedPref.getBoolean(KEY_AUTH_SKIPPED, false);
    }

    public void setAuthenticationSkipped(boolean skipped) {
        sharedPref.edit().putBoolean(KEY_AUTH_SKIPPED, skipped).apply();
    }

    public void clearUserInfo() {
        sharedPref.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_AUTH_SKIPPED)
                .apply();
    }
}