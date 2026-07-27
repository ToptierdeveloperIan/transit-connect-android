package com.example.imanicommunityapp.Network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Process-wide connectivity hub for the Imani app.
 *
 * <h2>Role</h2>
 * Owns a single {@link ConnectivityChecker} and fans out rich
 * {@link InternetStatus} updates to observers. Feature modules should prefer
 * this over constructing ad-hoc {@code ConnectivityChecker} instances so the
 * whole process shares one {@link android.net.ConnectivityManager.NetworkCallback}.
 *
 * <h2>Immediate offline detection</h2>
 * System callbacks update status as soon as the link changes; listeners are
 * notified on the <b>main thread</b> so UI can react without posting themselves.
 *
 * <h2>Isolation</h2>
 * This class lives only in {@code Network}. It does not import Fragments,
 * Navigation, Wallet, Settings, etc. Other modules call
 * {@link #getInstance(Context)} when they are ready (e.g. Application or screen).
 *
 * <h2>Pair with contextual offline</h2>
 * For place-aware UI use {@link ContextualOfflineController}, which combines
 * this monitor with {@link OfflineScopeTracker}.
 *
 * @see ConnectivityChecker
 * @see InternetStatusListener
 */
public final class NetworkMonitor {

    private static final String TAG = "NetworkMonitor";

    private static volatile NetworkMonitor instance;

    private final ConnectivityChecker connectivityChecker;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<InternetStatusListener> statusListeners = new CopyOnWriteArraySet<>();

    /** Last status we dispatched (for change detection / getCurrent). */
    private volatile InternetStatus lastStatus;

    private NetworkMonitor(@NonNull Context context) {
        // One checker for the process lifetime of this singleton.
        connectivityChecker = new ConnectivityChecker(context.getApplicationContext());
        lastStatus = connectivityChecker.getInternetStatus();

        // Bridge boolean + full status: ConnectivityChecker already tracks capabilities;
        // we poll getInternetStatus on each boolean tick AND register a full bridge below.
        connectivityChecker.addListener(isConnected -> {
            // Boolean path still useful; always re-read full status for LIMITED vs OFFLINE.
            publishIfChanged(connectivityChecker.getInternetStatus());
        });

        // Also expose full-status subscription on the checker when available.
        connectivityChecker.addInternetStatusListener(this::publishIfChanged);

        Log.i(TAG, "NetworkMonitor started; initial=" + describe(lastStatus));
    }

    /**
     * Lazy process singleton. Safe to call from Application or any screen.
     * Multiple calls with different contexts still share one instance (app context).
     */
    @NonNull
    public static NetworkMonitor getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) {
                    instance = new NetworkMonitor(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * @return singleton if already created; null if nothing has called
     * {@link #getInstance(Context)} yet (does not require Context).
     */
    @Nullable
    public static NetworkMonitor getInstanceOrNull() {
        return instance;
    }

    /**
     * Subscribe to full status updates. Immediate callback with current snapshot.
     */
    public void addStatusListener(@NonNull InternetStatusListener listener) {
        statusListeners.add(listener);
        final InternetStatus snapshot = getCurrentStatus();
        mainHandler.post(() -> listener.onInternetStatusChanged(snapshot));
    }

    public void removeStatusListener(@NonNull InternetStatusListener listener) {
        statusListeners.remove(listener);
    }

    /**
     * Latest known status (never null).
     */
    @NonNull
    public InternetStatus getCurrentStatus() {
        InternetStatus s = connectivityChecker.getInternetStatus();
        return s != null ? s : lastStatus;
    }

    /**
     * True only when state is {@link InternetState#ONLINE} (validated).
     */
    public boolean isOnline() {
        return getCurrentStatus().getState() == InternetState.ONLINE;
    }

    /**
     * True when offline or limited (not fully validated online).
     */
    public boolean isOfflineOrLimited() {
        InternetState state = getCurrentStatus().getState();
        return state == InternetState.OFFLINE || state == InternetState.LIMITED;
    }

    /**
     * Underlying checker for callers that only need {@link ConnectivityChecker#hasInternetConnection()}.
     * Prefer {@link #isOnline()} for validated internet.
     */
    @NonNull
    public ConnectivityChecker getConnectivityChecker() {
        return connectivityChecker;
    }

    private void publishIfChanged(@NonNull InternetStatus status) {
        InternetStatus previous = lastStatus;
        if (sameStatus(previous, status)) {
            return;
        }
        lastStatus = status;
        Log.d(TAG, "status " + describe(previous) + " → " + describe(status));
        mainHandler.post(() -> {
            for (InternetStatusListener listener : statusListeners) {
                try {
                    listener.onInternetStatusChanged(status);
                } catch (Exception ex) {
                    Log.e(TAG, "InternetStatusListener error", ex);
                }
            }
        });
    }

    private static boolean sameStatus(@Nullable InternetStatus a, @NonNull InternetStatus b) {
        if (a == null) return false;
        return a.getState() == b.getState()
                && a.getTransport() == b.getTransport()
                && a.getNetworkCost() == b.getNetworkCost();
    }

    @NonNull
    private static String describe(@Nullable InternetStatus s) {
        if (s == null) return "null";
        return s.getState() + "/" + s.getTransport() + "/" + s.getNetworkCost();
    }
}
