package com.example.imanicommunityapp.Sync.Cache;

// java


public final class DataSyncState {
    public final ConnectivityStatus connectivity;
    public final AuthStatus auth;
    public final CacheStatus cache;
    public final SyncActivity activity;
    public final boolean queuedForRetry;
    public final long lastSuccessfulSyncAt;
    public final String lastSyncMessage;

    public DataSyncState(
            ConnectivityStatus connectivity,
            AuthStatus auth,
            CacheStatus cache,
            SyncActivity activity,
            boolean queuedForRetry,
            long lastSuccessfulSyncAt,
            String lastSyncMessage
    ) {
        this.connectivity = connectivity;
        this.auth = auth;
        this.cache = cache;
        this.activity = activity;
        this.queuedForRetry = queuedForRetry;
        this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
        this.lastSyncMessage = lastSyncMessage;
    }

    public static DataSyncState withDefaults() {
        return new DataSyncState(
                ConnectivityStatus.UNKNOWN,
                AuthStatus.UNAUTHENTICATED,
                CacheStatus.EMPTY,
                SyncActivity.IDLE,
                false,
                0L,
                null
        );
    }
}
