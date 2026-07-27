package com.example.imanicommunityapp.Network;

/**
 * How aggressively the UI should react for the current {@link OfflineScope}
 * given the latest {@link InternetStatus}.
 *
 * <p>Resolved by {@link OfflinePolicy} — screens should not re-implement this matrix.
 */
public enum OfflineMode {

    /**
     * Full connectivity (validated online). Normal UI; primary actions enabled.
     */
    ONLINE,

    /**
     * Link present but not fully validated (captive portal / limited).
     * Some scopes treat this like offline-hard; others show a warning only.
     */
    DEGRADED,

    /**
     * Offline but the current screen may continue with local/cached work
     * (e.g. edit name offline, browse cached content). Show a non-blocking banner.
     */
    OFFLINE_SOFT,

    /**
     * Offline and the primary action for this screen must not run
     * (pay, OTP, redeem, come online). Block primary CTA; explain why.
     */
    OFFLINE_HARD
}
