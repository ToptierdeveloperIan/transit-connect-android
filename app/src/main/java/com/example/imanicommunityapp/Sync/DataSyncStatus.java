package com.example.imanicommunityapp.Sync;

import com.example.imanicommunityapp.Sync.Cache.BookingCacheState;

public enum DataSyncStatus {
    FRESH_UP_TO_DATE,
    STALE_UNCHANGED_READY_FOR_QUEUE,
    STALE_USER_UPDATED_READYFORQUEUE,

    IDLE,
    //This is the entry point of the state machine. The app is online and has a valid auth token, but no data has been fetched yet. This state can be used to trigger the initial data fetch when the app starts or when the user logs in.
    // No role assigned, so no data should be available. This is a special state that can be used to show a specific UI (e.g., "No access") and skip any data fetching logic.
    NO_ROLE_LOCAL,
    // the user is currently offline but we have sth locally stored. This can be used to show cached data with an "offline" badge, and to trigger a background sync when the connection is restored.
    OFFLINE_HAS_CACHE,
    // the user is currently offline and we have no data locally. This can be used to show an "offline" message and disable any data-dependent features until a connection is available.
    OFFLINE_NO_CACHE,
    //the user is online with no cache present.
    ONLINE_NO_CACHE_NOT_FETCHED,
    // This state should be removed
    ONLINE_NO_DATA_FOUND,

    ONLINE_CACHE_STALE,
    ONLINE_CACHE_FRESH,
    ONLINE_SYNC_IN_PROGRESS,
    QUEUED_FOR_RETRY,
    ERROR


    // This is the new implementation of the state machine. It is more granular and allows for better handling of different scenarios. The states are self-explanatory and can be used to trigger specific actions in the app (e.g., show a loading spinner, show an error message, etc.).


}
