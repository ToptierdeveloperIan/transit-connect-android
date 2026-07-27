package com.example.imanicommunityapp.Network;

import androidx.annotation.NonNull;

/**
 * Immutable snapshot of connectivity at a point in time.
 *
 * <p>Produced by {@link ConnectivityChecker} / {@link NetworkMonitor} and consumed
 * by {@link OfflinePolicy} for context-aware offline UI.
 *
 * <ul>
 *   <li>{@link InternetState#ONLINE} — validated internet</li>
 *   <li>{@link InternetState#LIMITED} — link without validation (e.g. captive portal)</li>
 *   <li>{@link InternetState#OFFLINE} — no usable network</li>
 * </ul>
 */
public final class InternetStatus {

    private final InternetState state;
    private final NetworkCost cost;
    private final NetworkTransport transport;

    public InternetStatus(
            @NonNull InternetState state,
            @NonNull NetworkTransport transport,
            @NonNull NetworkCost cost
    ) {
        this.state = state;
        this.cost = cost;
        this.transport = transport;
    }

    /**
     * @deprecated use {@link #getState()} — kept for existing call sites
     */
    @Deprecated
    @NonNull
    public InternetState getInternetstate() {
        return state;
    }

    @NonNull
    public InternetState getState() {
        return state;
    }

    @NonNull
    public NetworkCost getNetworkCost() {
        return cost;
    }

    @NonNull
    public NetworkTransport getTransport() {
        return transport;
    }

    /** True when state is {@link InternetState#ONLINE}. */
    public boolean isValidatedOnline() {
        return state == InternetState.ONLINE;
    }

    @Override
    @NonNull
    public String toString() {
        return "InternetStatus{state=" + state
                + ", transport=" + transport
                + ", cost=" + cost + '}';
    }
}
