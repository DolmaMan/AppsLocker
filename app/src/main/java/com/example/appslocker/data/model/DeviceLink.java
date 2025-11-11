package com.example.appslocker.data.model;

import java.util.HashMap;
import java.util.Map;

public class DeviceLink {
    private boolean active;
    private String parentDeviceId;
    private String childDeviceId;
    private String linkId;
    private Map<String, Long> blackListApps;

    public DeviceLink() {
        this.blackListApps = new HashMap<>();
    }

    public DeviceLink(String parentDeviceId, String childDeviceId) {
        this.parentDeviceId = parentDeviceId;
        this.childDeviceId = childDeviceId;
        this.active = true;
        this.blackListApps = new HashMap<>();
    }

    // Getters and Setters
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getParentDeviceId() { return parentDeviceId; }
    public void setParentDeviceId(String parentDeviceId) { this.parentDeviceId = parentDeviceId; }

    public String getChildDeviceId() { return childDeviceId; }
    public void setChildDeviceId(String childDeviceId) { this.childDeviceId = childDeviceId; }

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) { this.linkId = linkId; }

    public Map<String, Long> getBlackListApps() { return blackListApps; }
    public void setBlackListApps(Map<String, Long> blackListApps) { this.blackListApps = blackListApps; }

    // Вспомогательные методы для работы с черным списком
    public void addToBlackList(String packageName, long timestamp) {
        if (blackListApps == null) {
            blackListApps = new HashMap<>();
        }
        blackListApps.put(packageName, timestamp);
    }

    public void removeFromBlackList(String packageName) {
        if (blackListApps != null) {
            blackListApps.remove(packageName);
        }
    }

    public boolean isAppBlocked(String packageName) {
        return blackListApps != null && blackListApps.containsKey(packageName);
    }

    public long getBlockTime(String packageName) {
        if (blackListApps != null && blackListApps.containsKey(packageName)) {
            return blackListApps.get(packageName);
        }
        return 0L;
    }

    @Override
    public String toString() {
        return "DeviceLink{" +
                "active=" + active +
                ", parentDeviceId='" + parentDeviceId + '\'' +
                ", childDeviceId='" + childDeviceId + '\'' +
                ", linkId='" + linkId + '\'' +
                ", blackListApps=" + blackListApps +
                '}';
    }
}