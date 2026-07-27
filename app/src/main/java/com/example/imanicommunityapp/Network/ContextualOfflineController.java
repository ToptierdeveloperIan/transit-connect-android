package com.example.imanicommunityapp.Network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Combines <b>live network</b> + <b>where the user is</b> into {@link OfflineUiState}.
 *
 * <h2>The product behaviour you asked for</h2>
 * Mobile apps that instantly know they went offline, and change UI
 * <em>according to the current screen/flow</em> — not one generic banner only.
 *
 * <pre>
 * NetworkMonitor ──┐
 *                  ├── OfflinePolicy.resolve(scope, status) → OfflineUiState → UI
 * OfflineScopeTracker ──┘
 * </pre>
 *
 * <h2>Usage (feature screen — outside this package)</h2>
 * <pre>{@code
 * // onViewCreated / onResume:
 * OfflineScopeTracker.getInstance().enter(OfflineScope.SETTINGS_PHONE);
 * ContextualOfflineController c = ContextualOfflineController.getInstance(requireContext());
 * c.addListener(uiState -> {
 *     banner.setVisibility(uiState.shouldShowBanner() ? VISIBLE : GONE);
 *     banner.setText(uiState.getMessage());
 *     primaryButton.setEnabled(uiState.isPrimaryActionAllowed());
 * });
 *
 * // onDestroyView / onPause:
 * c.removeListener(...);
 * OfflineScopeTracker.getInstance().leave(OfflineScope.SETTINGS_PHONE);
 * }</pre>
 *
 * <h2>Isolation</h2>
 * No imports of UI toolkits beyond Android Context for {@link NetworkMonitor}.
 * No edits to Settings, Wallet, MainActivity, etc. are required for this module
 * to compile and run; wiring is opt-in by callers.
 *
 * @see NetworkMonitor
 * @see OfflineScopeTracker
 * @see OfflinePolicy
 */
public final class ContextualOfflineController {

    private static final String TAG = "ContextualOffline";

    /**
     * Listener for place-aware offline UI snapshots.
     * Delivered on the main thread.
     */
    public interface Listener {
        void onOfflineUiStateChanged(@NonNull OfflineUiState state);
    }

    private static volatile ContextualOfflineController instance;

    private final NetworkMonitor networkMonitor;
    private final OfflineScopeTracker scopeTracker;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private volatile OfflineUiState lastState;

    private final InternetStatusListener networkListener = status -> recomputeAndDispatch();
    private final OfflineScopeTracker.ScopeListener scopeListener = scope -> recomputeAndDispatch();

    private ContextualOfflineController(@NonNull Context context) {
        networkMonitor = NetworkMonitor.getInstance(context);
        scopeTracker = OfflineScopeTracker.getInstance();
        lastState = OfflinePolicy.resolve(
                scopeTracker.getCurrentScope(),
                networkMonitor.getCurrentStatus()
        );

        // Stay subscribed for the process lifetime of this singleton so first
        // listener always gets fresh transitions even if registered late.
        networkMonitor.addStatusListener(networkListener);
        scopeTracker.addListener(scopeListener);

        Log.i(TAG, "ContextualOfflineController ready; initial=" + lastState);
    }

    /**
     * Process-wide controller. Initializes {@link NetworkMonitor} if needed.
     */
    @NonNull
    public static ContextualOfflineController getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (ContextualOfflineController.class) {
                if (instance == null) {
                    instance = new ContextualOfflineController(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Nullable
    public static ContextualOfflineController getInstanceOrNull() {
        return instance;
    }

    /**
     * Register for updates. Immediate callback with the current resolved state
     * for the active {@link OfflineScope}.
     */
    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
        final OfflineUiState snapshot = getCurrentState();
        mainHandler.post(() -> listener.onOfflineUiStateChanged(snapshot));
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Latest resolved state (recomputed from current scope + network).
     */
    @NonNull
    public OfflineUiState getCurrentState() {
        return OfflinePolicy.resolve(
                scopeTracker.getCurrentScope(),
                networkMonitor.getCurrentStatus()
        );
    }

    /**
     * Force recompute (e.g. after tests inject status). Normally automatic.
     */
    public void refresh() {
        recomputeAndDispatch();
    }

    /**
     * Convenience for one-shot checks without listening
     * (e.g. before starting a network call).
     *
     * @param scope scope to evaluate (does not change the tracker stack)
     */
    @NonNull
    public OfflineUiState evaluate(@NonNull OfflineScope scope) {
        return OfflinePolicy.resolve(scope, networkMonitor.getCurrentStatus());
    }

    /**
     * True if primary action should be blocked for the <em>current</em> scope.
     */
    public boolean shouldBlockPrimaryAction() {
        return !getCurrentState().isPrimaryActionAllowed();
    }

    private void recomputeAndDispatch() {
        OfflineUiState next = OfflinePolicy.resolve(
                scopeTracker.getCurrentScope(),
                networkMonitor.getCurrentStatus()
        );
        OfflineUiState prev = lastState;
        if (Objects.equals(prev, next)) {
            return;
        }
        lastState = next;
        Log.d(TAG, "UI state " + prev + " → " + next);
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                try {
                    listener.onOfflineUiStateChanged(next);
                } catch (Exception ex) {
                    Log.e(TAG, "Listener error", ex);
                }
            }
        });
    }
}
