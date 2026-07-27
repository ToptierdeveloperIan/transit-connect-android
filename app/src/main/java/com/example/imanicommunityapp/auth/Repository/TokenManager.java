package com.example.imanicommunityapp.auth.Repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TokenManager {

    private static final String PREF_NAME = "user_prefs";
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String REFRESH_TOKEN_KEY = "refresh_token";
    private static final String USER_ROLE_KEY = "user_role";
    private static final String USER_ID = "user_id";
    private static final String TAG = "TokenManager";

    private final SharedPreferences sharedPreferences;

    public TokenManager(Context context) {
        sharedPreferences = createPreferences(context.getApplicationContext());
    }

    private SharedPreferences createPreferences(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.w(TAG, "Falling back to regular SharedPreferences", e);
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void saveAccessToken(String token) {
        sharedPreferences.edit().putString(ACCESS_TOKEN_KEY, token).apply();
    }

    public void saveRefreshToken(String token) {
        sharedPreferences.edit().putString(REFRESH_TOKEN_KEY, token).apply();
    }

    public void saveUserRole(String role) {
        sharedPreferences.edit().putString(USER_ROLE_KEY, role).apply();
    }

    public void saveUserID(String id) {
        sharedPreferences.edit().putString(USER_ID, id).apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null);
    }

    public String getUserRole() {
        return sharedPreferences.getString(USER_ROLE_KEY, null);
    }

    public String getUserID() {
        return sharedPreferences.getString(USER_ID, null);
    }

    public void clearAll() {
        sharedPreferences.edit()
                .remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
                .remove(USER_ROLE_KEY)
                .remove(USER_ID)
                .apply();
    }
}
