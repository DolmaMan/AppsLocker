package com.example.appslocker.data.model;

import java.util.HashMap;
import java.util.Map;

public class ChildDevice {
    private String deviceId;
    private String deviceName;
    private String linkId;
    private boolean isOnline;
    private Map<String, Long> blockedApps;

    public ChildDevice() {
        this.blockedApps = new HashMap<>();
    }

    public ChildDevice(String deviceId, String deviceName, String linkId) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.linkId = linkId;
        this.isOnline = false;
        this.blockedApps = new HashMap<>();
    }

    // Getters and Setters
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    // Изменено: возвращаем Map<String, Long> вместо int
    public Map<String, Long> getBlockedApps() { return blockedApps; }
    public void setBlockedApps(Map<String, Long> blockedApps) {
        this.blockedApps = blockedApps != null ? blockedApps : new HashMap<>();
    }

    // Вспомогательные методы для работы с blockedApps
    public int getBlockedAppsCount() {
        return blockedApps != null ? blockedApps.size() : 0;
    }

    public void addBlockedApp(String packageName, long timestamp) {
        if (blockedApps == null) {
            blockedApps = new HashMap<>();
        }
        blockedApps.put(packageName, timestamp);
    }

    public void removeBlockedApp(String packageName) {
        if (blockedApps != null) {
            blockedApps.remove(packageName);
        }
    }

    public boolean isAppBlocked(String packageName) {
        return blockedApps != null && blockedApps.containsKey(packageName);
    }

    public Long getBlockTime(String packageName) {
        if (blockedApps != null && blockedApps.containsKey(packageName)) {
            return blockedApps.get(packageName);
        }
        return null;
    }
}