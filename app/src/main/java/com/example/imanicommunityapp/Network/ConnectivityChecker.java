package com.example.imanicommunityapp.Network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-level sensor for device connectivity using
 * {@link ConnectivityManager.NetworkCallback}.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Register a default-network callback immediately on construction</li>
 *   <li>Derive {@link InternetState} (ONLINE / LIMITED / OFFLINE), transport, cost</li>
 *   <li>Notify boolean listeners and full {@link InternetStatusListener}s on the main thread</li>
 * </ul>
 *
 * <h2>Preferred entry point for app code</h2>
 * Prefer {@link NetworkMonitor#getInstance(Context)} so the process shares one
 * checker. Direct construction remains supported for legacy call sites
 * (Settings, ImaniApp) that already use this class.
 *
 * <h2>ONLINE vs LIMITED</h2>
 * ONLINE requires {@link NetworkCapabilities#NET_CAPABILITY_VALIDATED}.
 * LIMITED means a link exists but is not validated (captive portal, etc.).
 * Boolean {@link #hasInternetConnection()} is true only for ONLINE.
 */
public class ConnectivityChecker {

    private static final String TAG = "ConnectivityChecker";

    /**
     * Legacy simple listener: true only when fully ONLINE (validated).
     */
    public interface NetworkStatusListener {
        void onNetworkStatusChanged(boolean isConnected);
    }

    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback;
    private final Handler mainHandler;

    private InternetStatus internetStatus;
    private InternetState state;
    private NetworkCost cost;
    private NetworkTransport transport;

    private final Set<NetworkStatusListener> listeners = new CopyOnWriteArraySet<>();
    private final Set<InternetStatusListener> statusListeners = new CopyOnWriteArraySet<>();
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private volatile boolean internetAvailable;

    public ConnectivityChecker(Context context) {
        Context appContext = context.getApplicationContext();
        connectivityManager =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        state = InternetState.OFFLINE;
        transport = NetworkTransport.NONE;
        cost = NetworkCost.UNKNOWN;
        internetStatus = new InternetStatus(state, transport, cost);
        internetAvailable = false;
        refreshInternetStatus();

        /*
         * System fires these as soon as the default network changes — this is
         * what enables “immediately know we went offline” behaviour.
         */
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                refreshInternetStatus();
            }

            @Override
            public void onCapabilitiesChanged(
                    @NonNull Network network,
                    @NonNull NetworkCapabilities networkCapabilities
            ) {
                applyCapabilities(networkCapabilities);
            }

            @Override
            public void onLost(@NonNull Network network) {
                applyCapabilities(null);
            }

            @Override
            public void onUnavailable() {
                applyCapabilities(null);
            }
        };
        startMonitoring();
    }

    /**
     * @return true only if {@link InternetState#ONLINE} (validated internet)
     */
    public boolean hasInternetConnection() {
        return internetAvailable;
    }

    /**
     * Latest snapshot (state + transport + cost). Never null after construction.
     */
    @NonNull
    public InternetStatus getInternetStatus() {
        return internetStatus;
    }

    /**
     * Boolean ONLINE-only subscription. Immediate callback with current value.
     */
    public void addListener(@NonNull NetworkStatusListener listener) {
        listeners.add(listener);
        mainHandler.post(() -> listener.onNetworkStatusChanged(internetAvailable));
    }

    public void removeListener(@NonNull NetworkStatusListener listener) {
        listeners.remove(listener);
    }

    /**
     * Full-status subscription for contextual offline (LIMITED vs OFFLINE).
     * Immediate callback with current {@link InternetStatus}.
     *
     * @see NetworkMonitor
     */
    public void addInternetStatusListener(@NonNull InternetStatusListener listener) {
        statusListeners.add(listener);
        final InternetStatus snapshot = internetStatus;
        mainHandler.post(() -> listener.onInternetStatusChanged(snapshot));
    }

    public void removeInternetStatusListener(@NonNull InternetStatusListener listener) {
        statusListeners.remove(listener);
    }

    /**
     * Unregister callback and clear listeners. Prefer process-long lifetime via
     * {@link NetworkMonitor}; call only when tearing down tests or process.
     */
    public void shutdown() {
        listeners.clear();
        statusListeners.clear();
        if (connectivityManager == null) {
            return;
        }
        if (!monitoring.compareAndSet(true, false)) {
            return;
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (IllegalArgumentException ignored) {
            // Callback may already be unregistered if the process is being torn down.
        }
    }

    private void startMonitoring() {
        if (connectivityManager == null) {
            return;
        }
        if (!monitoring.compareAndSet(false, true)) {
            return;
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (RuntimeException ex) {
            Log.e(TAG, "Failed to register network callback", ex);
            monitoring.set(false);
        }
    }

    private void refreshInternetStatus() {
        applyCapabilities(readCurrentCapabilities());
    }

    /**
     * Recompute snapshot and notify listeners when anything material changes
     * (including LIMITED ↔ OFFLINE, which boolean listeners may not see).
     */
    private void applyCapabilities(@Nullable NetworkCapabilities capabilities) {
        InternetState newState = deriveInternetState(capabilities);
        NetworkTransport newTransport = deriveTransport(capabilities);
        NetworkCost newCost = deriveCost(capabilities);
        InternetStatus newStatus = new InternetStatus(newState, newTransport, newCost);

        boolean stateChanged = newState != state
                || newTransport != transport
                || newCost != cost;

        state = newState;
        transport = newTransport;
        cost = newCost;
        internetStatus = newStatus;

        boolean nowOnline = newState == InternetState.ONLINE;
        boolean onlineFlagChanged = nowOnline != internetAvailable;
        internetAvailable = nowOnline;

        if (!stateChanged && !onlineFlagChanged) {
            return;
        }

        Log.d(TAG, "capabilities → " + newState + " " + newTransport + " " + newCost);

        mainHandler.post(() -> {
            if (onlineFlagChanged) {
                for (NetworkStatusListener listener : listeners) {
                    try {
                        listener.onNetworkStatusChanged(internetAvailable);
                    } catch (Exception ex) {
                        Log.e(TAG, "NetworkStatusListener error", ex);
                    }
                }
            }
            // Always notify full listeners when state/transport/cost changed.
            if (stateChanged) {
                for (InternetStatusListener listener : statusListeners) {
                    try {
                        listener.onInternetStatusChanged(newStatus);
                    } catch (Exception ex) {
                        Log.e(TAG, "InternetStatusListener error", ex);
                    }
                }
            }
        });
    }

    @Nullable
    private NetworkCapabilities readCurrentCapabilities() {
        if (connectivityManager == null) {
            return null;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return null;
        }
        return connectivityManager.getNetworkCapabilities(activeNetwork);
    }

    private InternetState deriveInternetState(@Nullable NetworkCapabilities capabilities) {
        if (capabilities == null) {
            return InternetState.OFFLINE;
        }
        if (isValidated(capabilities)) {
            return InternetState.ONLINE;
        }
        // Has a network object/capabilities but not validated → LIMITED
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return InternetState.LIMITED;
        }
        return InternetState.OFFLINE;
    }

    private NetworkTransport deriveTransport(@Nullable NetworkCapabilities capabilities) {
        if (capabilities == null) {
            return NetworkTransport.NONE;
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkTransport.WIFI;
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkTransport.CELLULAR;
        }
        return NetworkTransport.NONE;
    }

    private NetworkCost deriveCost(@Nullable NetworkCapabilities capabilities) {
        if (capabilities == null) {
            return NetworkCost.UNKNOWN;
        }
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            return NetworkCost.UNMETERED;
        }
        return NetworkCost.METERED;
    }

    private boolean isValidated(@Nullable NetworkCapabilities capabilities) {
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
