package com.example.appslocker.core;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private FirebaseDatabase database;
    private boolean isInitialized = false;

    private FirebaseManager() {}

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public void initialize(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "Initializing Firebase...");

                // Автоматическая инициализация через google-services.json
                FirebaseApp.initializeApp(context);

                database = FirebaseDatabase.getInstance();

                // Включите persistence если нужно
                database.setPersistenceEnabled(true);

                isInitialized = true;
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                database = FirebaseDatabase.getInstance();
                isInitialized = true;
                Log.d(TAG, "Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
            isInitialized = false;
        }
    }

    public DatabaseReference getDatabaseReference(String path) {
        if (!isInitialized || database == null) {
            Log.e(TAG, "Firebase not initialized!");
            return null;
        }
        return database.getReference(path);
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}