package com.example.imanicommunityapp.Network;

import androidx.annotation.NonNull;

/**
 * Context-aware offline matrix: maps ({@link OfflineScope}, {@link InternetStatus})
 * → {@link OfflineUiState}.
 *
 * <p><b>Why a central policy?</b>
 * A generic “you are offline” banner is not enough. Payment, OTP, and name-edit
 * must react differently. This class is the single source of those rules so
 * screens stay declarative: observe state → bind UI.
 *
 * <p><b>Thread-safety:</b> pure functions; safe from any thread.
 *
 * <p><b>Extension:</b> add a new {@link OfflineScope} value and a case here.
 * Do not scatter offline string/logic across feature modules.
 *
 * @see ContextualOfflineController
 */
public final class OfflinePolicy {

    private OfflinePolicy() {
    }

    /**
     * Resolve UI guidance for the given place + live network snapshot.
     *
     * @param scope   where the user is (never null; use {@link OfflineScope#GENERIC})
     * @param status  latest connectivity snapshot (never null)
     * @return immutable UI state for banners / button enablement
     */
    @NonNull
    public static OfflineUiState resolve(
            @NonNull OfflineScope scope,
            @NonNull InternetStatus status
    ) {
        InternetState net = status.getState();

        // ----- Fully online -----
        if (net == InternetState.ONLINE) {
            return OfflineUiState.of(
                    scope,
                    OfflineMode.ONLINE,
                    status,
                    "",
                    "",
                    true,
                    true,
                    false
            );
        }

        // ----- Limited / captive / unvalidated -----
        if (net == InternetState.LIMITED) {
            return resolveLimited(scope, status);
        }

        // ----- True offline -----
        return resolveOffline(scope, status);
    }

    /**
     * LIMITED: link may exist but traffic is not validated.
     * Treat money/auth scopes as hard; browsing as degraded warning.
     */
    @NonNull
    private static OfflineUiState resolveLimited(
            @NonNull OfflineScope scope,
            @NonNull InternetStatus status
    ) {
        switch (scope) {
            case PAYMENT:
            case WALLET_DEPOSIT:
            case WALLET_SPEND:
            case AUTH_LOGIN:
            case AUTH_SIGNUP:
            case SETTINGS_PHONE:
            case PROMO_REDEEM:
            case CHECKOUT:
            case TERMS_ACCEPT:
            case DRIVER_LIVE:
                return OfflineUiState.of(
                        scope,
                        OfflineMode.OFFLINE_HARD,
                        status,
                        "Connection limited",
                        hardMessage(scope) + " Your network may require sign-in (captive portal).",
                        false,
                        true,
                        true
                );
            case SETTINGS_NAME:
                // Name can still be edited offline / queued — warn only.
                return OfflineUiState.of(
                        scope,
                        OfflineMode.DEGRADED,
                        status,
                        "Limited connection",
                        "You can edit your name; sync may wait until the connection is fully online.",
                        true,
                        true,
                        true
                );
            default:
                return OfflineUiState.of(
                        scope,
                        OfflineMode.DEGRADED,
                        status,
                        "Limited connection",
                        "Some features may not work until you are fully online.",
                        true,
                        true,
                        true
                );
        }
    }

    /**
     * OFFLINE: no usable validated internet.
     */
    @NonNull
    private static OfflineUiState resolveOffline(
            @NonNull OfflineScope scope,
            @NonNull InternetStatus status
    ) {
        switch (scope) {
            // ---- Soft: local work allowed, primary sync later ----
            case SETTINGS_NAME:
                return soft(
                        scope, status,
                        "You're offline",
                        "You can still change your name. It will sync when you are back online."
                );
            case HOME_RIDER:
                return soft(
                        scope, status,
                        "You're offline",
                        "Map and routes may be out of date. Connect to book or refresh fares."
                );
            case WALLET_BROWSE:
                return soft(
                        scope, status,
                        "You're offline",
                        "Showing last known balance if available. Deposits need a connection."
                );
            case TERMS_BROWSE:
                return soft(
                        scope, status,
                        "You're offline",
                        "You can read cached terms. Accepting a new version requires a connection."
                );
            case SETTINGS_HUB:
            case GENERIC:
            case SPLASH:
            case SELECT_STOP:
                return soft(
                        scope, status,
                        "You're offline",
                        "Some actions need a connection. You can keep browsing what is already on the device."
                );

            // ---- Hard: block primary network action ----
            case AUTH_LOGIN:
            case AUTH_SIGNUP:
                return hard(
                        scope, status,
                        "You're offline",
                        "Sign-in needs a network connection to send and verify your code."
                );
            case SETTINGS_PHONE:
                return hard(
                        scope, status,
                        "You're offline",
                        "Changing your phone number requires a connection to send a verification code."
                );
            case CHECKOUT:
                return hard(
                        scope, status,
                        "You're offline",
                        "Checkout needs a connection to get a live fare quote from the server."
                );
            case PAYMENT:
                return hard(
                        scope, status,
                        "You're offline",
                        "Payments require a connection so we can confirm with the payment provider."
                );
            case WALLET_DEPOSIT:
                return hard(
                        scope, status,
                        "You're offline",
                        "Deposits via M-Pesa or Airtel Money require a network connection."
                );
            case WALLET_SPEND:
                return hard(
                        scope, status,
                        "You're offline",
                        "Paying from your wallet needs a connection to settle the fare securely."
                );
            case TERMS_ACCEPT:
                return hard(
                        scope, status,
                        "You're offline",
                        "You must be online to accept the current Terms of Service."
                );
            case PROMO_REDEEM:
                return hard(
                        scope, status,
                        "You're offline",
                        "Redeeming a promo code requires a connection to validate the code."
                );
            case HOME_DRIVER:
                return hard(
                        scope, status,
                        "You're offline",
                        "Going online as a driver requires a connection."
                );
            case DRIVER_LIVE:
            case RIDE_ACTIVE:
                return hard(
                        scope, status,
                        "You're offline",
                        "Live trip updates need a connection. Trying to reconnect…"
                );
            default:
                return soft(
                        scope, status,
                        "You're offline",
                        "Connect to the internet to use all features."
                );
        }
    }

    @NonNull
    private static OfflineUiState soft(
            OfflineScope scope,
            InternetStatus status,
            String title,
            String message
    ) {
        return OfflineUiState.of(
                scope,
                OfflineMode.OFFLINE_SOFT,
                status,
                title,
                message,
                /* primary: allow local / queueable paths; screen decides */ true,
                true,
                true
        );
    }

    @NonNull
    private static OfflineUiState hard(
            OfflineScope scope,
            InternetStatus status,
            String title,
            String message
    ) {
        return OfflineUiState.of(
                scope,
                OfflineMode.OFFLINE_HARD,
                status,
                title,
                message,
                false,
                true,
                true
        );
    }

    @NonNull
    private static String hardMessage(@NonNull OfflineScope scope) {
        switch (scope) {
            case PAYMENT:
                return "Payments need a reliable connection.";
            case WALLET_DEPOSIT:
                return "Wallet deposits need a reliable connection.";
            case WALLET_SPEND:
                return "Wallet payment needs a reliable connection.";
            case AUTH_LOGIN:
            case AUTH_SIGNUP:
                return "Authentication needs a reliable connection.";
            case SETTINGS_PHONE:
                return "Phone verification needs a reliable connection.";
            case CHECKOUT:
                return "Checkout needs a reliable connection.";
            case PROMO_REDEEM:
                return "Promo validation needs a reliable connection.";
            case TERMS_ACCEPT:
                return "Accepting terms needs a reliable connection.";
            case DRIVER_LIVE:
                return "Live driver updates need a reliable connection.";
            default:
                return "This action needs a reliable connection.";
        }
    }
}
