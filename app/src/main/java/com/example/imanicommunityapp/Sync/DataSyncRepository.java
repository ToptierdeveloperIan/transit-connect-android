package com.example.imanicommunityapp.Sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.auth.DataLayer.UserProfileRoomDb;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataSyncRepository {
    private static final String BOOKING_DATA_TYPE = "booking_reference_data";
    private static final long STALE_AFTER_MS = 6 * 60 * 60 * 1000L;


    private final UserProfileRoomDb userProfileRoomDb;
    private final ConnectivityChecker connectivityChecker;
    private final ExecutorService databaseExecutor;
    private final Handler mainHandler;

    public DataSyncRepository(Context context) {
        // application context is used to avoid memory leaks in case the passed context is an Activity or Service
        Context appContext = context.getApplicationContext();
        // room database instance is obtained using the application context
        userProfileRoomDb = UserProfileRoomDb.getInstance(appContext);
        //connectivity checker instance is created using the application context
        connectivityChecker = new ConnectivityChecker(appContext);
        // a single-threaded executor is created for database operations to ensure they are executed sequentially
        databaseExecutor = Executors.newSingleThreadExecutor();
        // a handler is created to post results back to the main thread, allowing UI updates or callbacks to be executed on the main thread
        mainHandler = new Handler(Looper.getMainLooper());
    }


    // method to fetch User role for data OFFLINE SYNC SYSTEM
    public void getUserRole(UserRoleCallback callback) {
        databaseExecutor.execute(() -> {
            String userRole = userProfileRoomDb.userProfileDao().getUserRole();
            UserRoleResult result;

            if (userRole == null || userRole.trim().isEmpty()) {
                result = UserRoleResult.failure("User role not found in local storage.");
            } else {
                result = UserRoleResult.success(userRole.trim());
            }

            mainHandler.post(() -> callback.onResult(result));
        });
    }
    //Method for checking
    public void assessBookingDataSyncState(RepositoryCallback callback) {
        databaseExecutor.execute(() -> {
            String userRole = userProfileRoomDb.userProfileDao().getUserRole();
            // this is inefficient because it is not dynamic it only fetches current state
            boolean hasInternet = connectivityChecker.hasInternetConnection();
            UserProfileRoomDb.SyncMetadataEntity metadata = getOrCreateSyncMetadata();
            // start of block
            //Passing the data for evaluation for the first time
            SyncAssessmentResult result = evaluateSyncState(userRole, hasInternet, metadata);
            //if condition to update value high
            if (result.getStatus() == DataSyncStatus.OFFLINE_NO_CACHE) {
                metadata.queuedForRetry = true;
                metadata.lastSyncMessage = "Queued booking data sync until internet is available.";
                userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
                //passing data for evaluation the second time
                //end of block
                result = evaluateSyncState(userRole, hasInternet, metadata);
            }

            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // Method for saving data to local storage
    public void saveInitialData(BookingCallback callback,) {
        databaseExecutor.execute(() -> {

        });
    }

    @NonNull
    public ConnectivityChecker.NetworkStatusListener startMonitoringSyncState(
            @NonNull RepositoryCallback callback
    ) {
        ConnectivityChecker.NetworkStatusListener listener =
                isConnected -> assessBookingDataSyncState(callback);
        connectivityChecker.addListener(listener);
        return listener;
    }

    public void stopMonitoringSyncState(
            @Nullable ConnectivityChecker.NetworkStatusListener listener
    ) {
        if (listener == null) {
            return;
        }
        connectivityChecker.removeListener(listener);
    }

    public void shutdown() {
        connectivityChecker.shutdown();
        databaseExecutor.shutdown();
    }

    public void markSyncStarted() {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.SyncMetadataEntity metadata = getOrCreateSyncMetadata();
            metadata.fetchAttempted = true;
            metadata.syncInProgress = true;
            metadata.queuedForRetry = false;
            metadata.lastSyncMessage = "Sync in progress.";
            userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
        });
    }

    public void markLocalBookingDataStored() {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.SyncMetadataEntity metadata = getOrCreateSyncMetadata();
            metadata.localDataPresent = true;
            metadata.fetchAttempted = true;
            metadata.syncInProgress = false;
            metadata.queuedForRetry = false;
            metadata.lastSuccessfulSyncAt = System.currentTimeMillis();
            metadata.lastSyncMessage = "Local booking data is available.";
            userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
        });
    }
//This Function is used to mark the state when no data is found after a sync attempt. It updates the sync metadata in the local database to reflect that no local data is present, the fetch has been attempted, and the sync is not currently in progress. It also sets a message indicating that no booking data was returned by the backend. Most likely an error handling
    public void markNoDataFound() {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.SyncMetadataEntity metadata = getOrCreateSyncMetadata();
            metadata.localDataPresent = false;
            metadata.fetchAttempted = true;
            metadata.syncInProgress = false;
            metadata.lastSyncMessage = "No booking data was returned by the backend.";
            userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
        });
    }

    public void queueSyncForRetry(@NonNull String message) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.SyncMetadataEntity metadata = getOrCreateSyncMetadata();
            metadata.syncInProgress = false;
            metadata.queuedForRetry = true;
            metadata.lastSyncMessage = message;
            userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
        });
    }

    private SyncAssessmentResult evaluateSyncState(
            @Nullable String userRole,
            // should return and object
            boolean hasInternet,
            @NonNull UserProfileRoomDb.SyncMetadataEntity metadata
    ) {

        // checks user role
        //checks if user is local storage
        if (userRole == null || userRole.trim().isEmpty()) {
            // If no user role exists, treat cached booking data as not present.
            return new SyncAssessmentResult(
                    DataSyncStatus.NO_ROLE_LOCAL,
                    null,
                    hasInternet,
                    false, // localDataPresent -> false
                    false, // queuedForRetry -> false
                    "User role not found in local storage."
            );
        }
// Sync In progress
        if (metadata.syncInProgress) {
            return new SyncAssessmentResult(
                    DataSyncStatus.ONLINE_SYNC_IN_PROGRESS,
                    userRole.trim(),
                    hasInternet,
                    metadata.localDataPresent,
                    metadata.queuedForRetry,
                    "Booking data sync is already running."
            );
        }

        // if internet isnt present and Local unsynced data is present
        if (!hasInternet && metadata.localDataPresent) {
            return new SyncAssessmentResult(
                    DataSyncStatus.OFFLINE_HAS_CACHE,
                    userRole.trim(),
                    false,
                    true,
                    metadata.queuedForRetry,
                    "Offline mode: using cached booking data."
            );
        }

        if (!hasInternet) {
            return new SyncAssessmentResult(
                    metadata.queuedForRetry ? DataSyncStatus.QUEUED_FOR_RETRY : DataSyncStatus.OFFLINE_NO_CACHE,
                    userRole.trim(),
                    false,
                    metadata.localDataPresent,
                    metadata.queuedForRetry,
                    metadata.queuedForRetry
                            ? "Booking data sync is queued until internet is available."
                            : "No internet connection and no cached booking data."
            );
        }

        if (!metadata.localDataPresent && !metadata.fetchAttempted) {
            return new SyncAssessmentResult(
                    DataSyncStatus.ONLINE_NO_CACHE_NOT_FETCHED,
                    userRole.trim(),
                    true,
                    false,
                    metadata.queuedForRetry,
                    "Internet is available. Booking data should be fetched."
            );
        }

        if (!metadata.localDataPresent) {
            return new SyncAssessmentResult(
                    DataSyncStatus.ONLINE_NO_DATA_FOUND,
                    userRole.trim(),
                    true,
                    false,
                    metadata.queuedForRetry,
                    metadata.lastSyncMessage != null
                            ? metadata.lastSyncMessage
                            : "No local booking data is available."
            );
        }

        if (isStale(metadata.lastSuccessfulSyncAt)) {
            return new SyncAssessmentResult(
                    DataSyncStatus.ONLINE_CACHE_STALE,
                    userRole.trim(),
                    true,
                    true,
                    metadata.queuedForRetry,
                    "Cached booking data needs updating."
            );
        }

        return new SyncAssessmentResult(
                DataSyncStatus.ONLINE_CACHE_FRESH,
                userRole.trim(),
                true,
                true,
                metadata.queuedForRetry,
                "Cached booking data is fresh."
        );
    }

    //STALE DATA CHECKS
    private boolean isStale(long lastSuccessfulSyncAt) {
        if (lastSuccessfulSyncAt <= 0L) {
            return true;
        }
        return System.currentTimeMillis() - lastSuccessfulSyncAt > STALE_AFTER_MS;
    }

    @NonNull
    private UserProfileRoomDb.SyncMetadataEntity getOrCreateSyncMetadata() {
        UserProfileRoomDb.SyncMetadataEntity metadata = userProfileRoomDb.syncMetadataDao().getSyncMetadata();
        if (metadata != null) {
            return metadata;
        }

        metadata = new UserProfileRoomDb.SyncMetadataEntity(
                BOOKING_DATA_TYPE,
                false,
                false,
                false,
                false,
                0L,
                null
        );
        userProfileRoomDb.syncMetadataDao().saveSyncMetadata(metadata);
        return metadata;
    }

}
