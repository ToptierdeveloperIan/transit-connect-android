package com.example.imanicommunityapp;

import android.app.Application;
import android.util.Log;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;
import com.example.imanicommunityapp.settings.sync.ProfileNameQueue;

/**
 * Process-wide application entry. Runs before any Activity.
 *
 * <p>Warms the backend network hub so TokenManager and Retrofit clients are ready
 * for cold paths (receivers, workers, early services) without waiting for UI.
 *
 * <p>Also registers connectivity monitoring to drain offline PROFILE_NAME_UPDATE
 * events when the device comes online (phone changes are never queued).
 */
public class ImaniApp extends Application {

    private static final String TAG = "ImaniApp";

    @Override
    public void onCreate() {
        super.onCreate();
        // Bind TokenManager + ensure hub can serve getClient() / authenticated traffic.
        authRetrofitClient.init(this);
        // Eagerly build authenticated Retrofit so first API call pays no init cost.
        authRetrofitClient.getClient(this);
        Log.d(TAG, "authRetrofitClient initialized (plain + authenticated hub ready)");

        // Offline name queue: push when network returns.
        ConnectivityChecker connectivity = new ConnectivityChecker(this);
        connectivity.addListener(isConnected -> {
            if (isConnected) {
                Log.d(TAG, "Network up — draining profile name queue");
                ProfileNameQueue.getInstance(this).drain(() -> { });
            }
        });
        if (connectivity.hasInternetConnection()) {
            ProfileNameQueue.getInstance(this).drain(() -> { });
        }
    }
}
