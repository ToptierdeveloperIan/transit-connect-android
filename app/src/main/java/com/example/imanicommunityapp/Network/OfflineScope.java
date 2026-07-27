package com.example.imanicommunityapp.Network;

/**
 * Where the user currently is in the product — drives <em>contextual</em> offline UI.
 *
 * <p>This is intentionally <b>not</b> a nav graph ID. Screens (or a future nav binder
 * outside this package) map destinations → scopes. Keeping scopes in {@code Network}
 * allows offline policy to live next to connectivity without coupling to Fragments.
 *
 * <p><b>Usage (from a screen, without this package calling into UI):</b>
 * <pre>{@code
 * OfflineScopeTracker.getInstance().enter(OfflineScope.SETTINGS_PHONE);
 * // ... onDestroyView / onPause:
 * OfflineScopeTracker.getInstance().leave(OfflineScope.SETTINGS_PHONE);
 * }</pre>
 *
 * @see OfflinePolicy
 * @see OfflineScopeTracker
 */
public enum OfflineScope {

    /**
     * Fallback when no screen has registered a scope.
     * Policy: soft banner only — do not block the whole app.
     */
    GENERIC,

    /** Splash / cold start routing. */
    SPLASH,

    /** OTP / login — hard dependency on network for request/verify. */
    AUTH_LOGIN,

    /** Signup / register. */
    AUTH_SIGNUP,

    /** Rider home map / browse. Offline: soft warning, local cache OK. */
    HOME_RIDER,

    /** Driver home / availability. Going online needs network. */
    HOME_DRIVER,

    /** Route / stop selection before checkout. */
    SELECT_STOP,

    /** Fare quote / checkout submit — needs network for server quote. */
    CHECKOUT,

    /** M-Pesa / STK / wallet pay — hard offline block. */
    PAYMENT,

    /** In-wallet deposit (M-Pesa / Airtel). */
    WALLET_DEPOSIT,

    /** Pay fare from wallet balance. */
    WALLET_SPEND,

    /** Wallet balance / ledger browse (can show last cached soft). */
    WALLET_BROWSE,

    /** Change display name — soft offline (queueable). */
    SETTINGS_NAME,

    /** Change phone — hard offline (OTP). */
    SETTINGS_PHONE,

    /** Settings hub. */
    SETTINGS_HUB,

    /** Terms of Service accept gate — accept needs network; read may be soft. */
    TERMS_ACCEPT,

    /** Terms read-only from settings. */
    TERMS_BROWSE,

    /** Promo redeem — typically online-only. */
    PROMO_REDEEM,

    /** Driver live location / websocket. */
    DRIVER_LIVE,

    /** Active trip tracking. */
    RIDE_ACTIVE
}
