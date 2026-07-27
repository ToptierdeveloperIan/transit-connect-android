package com.example.imanicommunityapp.Sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SyncAssessmentResult {
    @NonNull
    private final DataSyncStatus status;
    @Nullable
    private final String userRole;
    private final boolean internetAvailable;
    private final boolean localDataPresent;
    private final boolean queuedForRetry;
    @Nullable
    private final String message;

    public SyncAssessmentResult(
            @NonNull DataSyncStatus status,
            @Nullable String userRole,
            boolean internetAvailable,
            boolean localDataPresent,
            boolean queuedForRetry,
            @Nullable String message
    ) {
        this.status = status;
        this.userRole = userRole;
        this.internetAvailable = internetAvailable;
        this.localDataPresent = localDataPresent;
        this.queuedForRetry = queuedForRetry;
        this.message = message;
    }

    @NonNull
    public DataSyncStatus getStatus() {
        return status;
    }

    @Nullable
    public String getUserRole() {
        return userRole;
    }

    public boolean isInternetAvailable() {
        return internetAvailable;
    }

    public boolean isLocalDataPresent() {
        return localDataPresent;
    }

    public boolean isQueuedForRetry() {
        return queuedForRetry;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}

