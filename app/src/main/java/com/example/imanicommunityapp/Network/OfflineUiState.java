package com.example.imanicommunityapp.Network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Immutable UI-oriented snapshot: “given where the user is + network, what should UI do?”
 *
 * <p>Produced only by {@link OfflinePolicy#resolve(OfflineScope, InternetStatus)}.
 * Screens bind banners, enable/disable buttons, and copy from these fields —
 * they should not invent their own offline strings per ad-hoc if-checks.
 *
 * <p>This class lives in {@code Network} so policy and connectivity stay co-located.
 * No Android View types here (pure model) so Compose and Views can both consume it.
 */
public final class OfflineUiState {

    private final OfflineScope scope;
    private final OfflineMode mode;
    private final InternetStatus networkStatus;

    /** Short title for banner / snackbar (may be empty when ONLINE). */
    private final String title;

    /** Longer explanation for the user. */
    private final String message;

    /** Whether the screen’s primary network-bound action should be enabled. */
    private final boolean primaryActionAllowed;

    /** Whether secondary / local-only actions stay allowed (e.g. edit fields, scroll). */
    private final boolean localInteractionAllowed;

    /** Hint to show a persistent top/bottom offline banner. */
    private final boolean showBanner;

    private OfflineUiState(
            @NonNull OfflineScope scope,
            @NonNull OfflineMode mode,
            @NonNull InternetStatus networkStatus,
            @NonNull String title,
            @NonNull String message,
            boolean primaryActionAllowed,
            boolean localInteractionAllowed,
            boolean showBanner
    ) {
        this.scope = scope;
        this.mode = mode;
        this.networkStatus = networkStatus;
        this.title = title;
        this.message = message;
        this.primaryActionAllowed = primaryActionAllowed;
        this.localInteractionAllowed = localInteractionAllowed;
        this.showBanner = showBanner;
    }

    @NonNull
    public static OfflineUiState of(
            @NonNull OfflineScope scope,
            @NonNull OfflineMode mode,
            @NonNull InternetStatus networkStatus,
            @NonNull String title,
            @NonNull String message,
            boolean primaryActionAllowed,
            boolean localInteractionAllowed,
            boolean showBanner
    ) {
        return new OfflineUiState(
                scope, mode, networkStatus, title, message,
                primaryActionAllowed, localInteractionAllowed, showBanner
        );
    }

    @NonNull
    public OfflineScope getScope() {
        return scope;
    }

    @NonNull
    public OfflineMode getMode() {
        return mode;
    }

    @NonNull
    public InternetStatus getNetworkStatus() {
        return networkStatus;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public boolean isPrimaryActionAllowed() {
        return primaryActionAllowed;
    }

    public boolean isLocalInteractionAllowed() {
        return localInteractionAllowed;
    }

    public boolean shouldShowBanner() {
        return showBanner;
    }

    /** Convenience: true when mode is ONLINE. */
    public boolean isFullyOnline() {
        return mode == OfflineMode.ONLINE;
    }

    /** Convenience: hard block on primary CTA. */
    public boolean isHardOffline() {
        return mode == OfflineMode.OFFLINE_HARD;
    }

    /** Convenience: soft offline or degraded with banner. */
    public boolean isSoftRestriction() {
        return mode == OfflineMode.OFFLINE_SOFT || mode == OfflineMode.DEGRADED;
    }

    @Override
    @NonNull
    public String toString() {
        return "OfflineUiState{"
                + "scope=" + scope
                + ", mode=" + mode
                + ", network=" + networkStatus.getState()
                + ", primaryAllowed=" + primaryActionAllowed
                + ", banner=" + showBanner
                + ", title='" + title + '\''
                + '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof OfflineUiState)) return false;
        OfflineUiState that = (OfflineUiState) o;
        return primaryActionAllowed == that.primaryActionAllowed
                && localInteractionAllowed == that.localInteractionAllowed
                && showBanner == that.showBanner
                && scope == that.scope
                && mode == that.mode
                && title.equals(that.title)
                && message.equals(that.message)
                && networkStatus.getState() == that.networkStatus.getState()
                && networkStatus.getTransport() == that.networkStatus.getTransport()
                && networkStatus.getNetworkCost() == that.networkStatus.getNetworkCost();
    }

    @Override
    public int hashCode() {
        int result = scope.hashCode();
        result = 31 * result + mode.hashCode();
        result = 31 * result + networkStatus.getState().hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (primaryActionAllowed ? 1 : 0);
        result = 31 * result + (localInteractionAllowed ? 1 : 0);
        result = 31 * result + (showBanner ? 1 : 0);
        return result;
    }
}
