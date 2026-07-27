package com.example.imanicommunityapp.Sync.Rehydration;

public enum RehydrationTrigger {
    APP_START,
    USER_LOGIN,
    NETWORK_RESTORED,
    PERIODIC_CHECK,
    ON_FOREGROUND,      // app moved to foreground
    ON_COLD_START,      // fresh process start
    ON_CONNECTIVITY,    // regained connectivity
    ON_LOGIN,           // user authenticated / role changed
    ON_MANUAL,          // user pull-to-refresh or explicit request
    ON_BACKGROUND_JOB,  // periodic WorkManager job
    ON_DATA_MUTATION    // local create/update/delete that may require sync
}
