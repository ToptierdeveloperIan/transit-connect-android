package com.example.imanicommunityapp.Network;

import androidx.annotation.NonNull;

/**
 * Listener that receives the full {@link InternetStatus} snapshot whenever
 * connectivity capabilities change (online / limited / offline, transport, cost).
 *
 * <p>Prefer this over the boolean {@link ConnectivityChecker.NetworkStatusListener}
 * when building context-aware offline UI — LIMITED vs OFFLINE can matter by screen.
 *
 * <p><b>Threading:</b> callbacks are delivered on the main thread.
 *
 * @see NetworkMonitor
 * @see ContextualOfflineController
 */
public interface InternetStatusListener {

    /**
     * Invoked immediately on registration (current snapshot) and on every
     * meaningful connectivity change thereafter.
     *
     * @param status immutable snapshot of link state; never null
     */
    void onInternetStatusChanged(@NonNull InternetStatus status);
}
