package com.example.appslocker.data.model;


import java.util.ArrayList;
import java.util.List;

public class FamilyGroup {
    private String parentId;
    private List<String> childIds;

    public FamilyGroup() {
        childIds = new ArrayList<>();
    }

    public FamilyGroup(String parentId) {
        this.parentId = parentId;
        this.childIds = new ArrayList<>();
    }


    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public List<String> getChildIds() { return childIds; }
    public void setChildIds(List<String> childIds) { this.childIds = childIds; }

    public void addChild(String childId) {
        if (!childIds.contains(childId)) {
            childIds.add(childId);
        }
    }

    public void removeChild(String childId) {
        childIds.remove(childId);
    }
}