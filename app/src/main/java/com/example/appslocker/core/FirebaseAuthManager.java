package com.example.appslocker.core;

import android.content.Context;
import android.util.Log;

import com.example.appslocker.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.List;

public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static FirebaseAuthManager instance;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private List<AuthListener> authListeners = new ArrayList<>();

    public interface AuthListener {
        void onAuthSuccess(FirebaseUser user);
        void onAuthError(String error);
        void onAuthSignedOut();
    }

    private FirebaseAuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();

        // Слушаем изменения состояния аутентификации
        firebaseAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                Log.d(TAG, "Auth state changed: user signed in - " + user.getEmail());
            } else {
                Log.d(TAG, "Auth state changed: user signed out");
                notifyAuthSignedOut();
            }
        });
    }

    public static FirebaseAuthManager getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthManager();
        }
        return instance;
    }

    public void initializeGoogleSignIn(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(Exception.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            } else {
                notifyAuthError("Не удалось получить данные аккаунта");
            }
        } catch (Exception e) {
            Log.e(TAG, "Google sign-in failed", e);
            notifyAuthError("Ошибка входа: " + e.getMessage());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        notifyAuthSuccess(user);
                    } else {
                        notifyAuthError("Ошибка аутентификации Firebase");
                    }
                });
    }

    public void signInAnonymously(AuthListener listener) {
        addAuthListener(listener);
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        notifyAuthSuccess(user);
                    } else {
                        notifyAuthError("Ошибка анонимного входа");
                    }
                    removeAuthListener(listener);
                });
    }

    public void signOut() {
        firebaseAuth.signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
        notifyAuthSignedOut();
    }

    // Уведомления для слушателей
    private void notifyAuthSuccess(FirebaseUser user) {
        for (AuthListener listener : new ArrayList<>(authListeners)) {
            listener.onAuthSuccess(user);
        }
    }

    private void notifyAuthError(String error) {
        for (AuthListener listener : new ArrayList<>(authListeners)) {
            listener.onAuthError(error);
        }
    }

    private void notifyAuthSignedOut() {
        for (AuthListener listener : new ArrayList<>(authListeners)) {
            listener.onAuthSignedOut();
        }
    }

    // Управление слушателями
    public void addAuthListener(AuthListener listener) {
        if (!authListeners.contains(listener)) {
            authListeners.add(listener);
        }
    }

    public void removeAuthListener(AuthListener listener) {
        authListeners.remove(listener);
    }

    public void clearAuthListeners() {
        authListeners.clear();
    }

    // Геттеры
    public boolean isUserSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public boolean isAnonymousUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isAnonymous();
    }

    public boolean isGoogleUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && !user.isAnonymous();
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public String getCurrentUserName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    public GoogleSignInClient getGoogleSignInClient() {
        return googleSignInClient;
    }
}