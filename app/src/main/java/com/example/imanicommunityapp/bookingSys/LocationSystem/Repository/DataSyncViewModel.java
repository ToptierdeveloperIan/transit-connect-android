package com.example.imanicommunityapp.bookingSys.LocationSystem.Repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.Sync.DataSyncRepository;
import com.example.imanicommunityapp.Sync.DataSyncStatus;
import com.example.imanicommunityapp.Sync.SyncAssessmentResult;

public class DataSyncViewModel extends AndroidViewModel {

    private final DataSyncRepository dataSyncRepository;
    private final ConnectivityChecker.NetworkStatusListener syncStateListener;
    //Enum for data syncing
    private final MutableLiveData<DataSyncStatus> syncStatus = new MutableLiveData<>(DataSyncStatus.IDLE);
    //Data Sycning boolean
    private final MutableLiveData<Boolean> syncing = new MutableLiveData<>(false);
    // this checks if we have lcocaaly stored data
    private final MutableLiveData<Boolean> cachedBookingDataAvailable = new MutableLiveData<>(false);
    // sync completed
    private final MutableLiveData<Boolean> syncCompleted = new MutableLiveData<>(false);
    // the last day we performed a sync operation
    private final MutableLiveData<Long> lastSuccessfulSyncAt = new MutableLiveData<>(0L);
    //result from sync operation
    private final MutableLiveData<String> syncMessage = new MutableLiveData<>();
    // returns user role
    private final MutableLiveData<String> userRole = new MutableLiveData<>();
    //instant creation
    public DataSyncViewModel(@NonNull Application application) {
        super(application);
        dataSyncRepository = new DataSyncRepository(application);
        syncing.setValue(true);
        syncStateListener = dataSyncRepository.startMonitoringSyncState(this::applySyncAssessment);
    }

    public LiveData<Boolean> getSyncing() {
        return syncing;
    }

    public LiveData<DataSyncStatus> getSyncStatus() {
        return syncStatus;
    }

    public LiveData<Boolean> getCachedBookingDataAvailable() {
        return cachedBookingDataAvailable;
    }

    public LiveData<Boolean> getSyncCompleted() {
        return syncCompleted;
    }

    public LiveData<Long> getLastSuccessfulSyncAt() {
        return lastSuccessfulSyncAt;
    }

    public LiveData<String> getSyncMessage() {
        return syncMessage;
    }

    public LiveData<String> getUserRole() {
        return userRole;
    }


    //loading user data from the repo
    public void loadUserRole() {
        syncing.setValue(true);
        dataSyncRepository.getUserRole(result -> {
            syncing.setValue(false);

            if (result.isSuccess()) {
                userRole.setValue(result.getUserRole());
                syncMessage.setValue(null);
                return;
            }

            userRole.setValue(null);
            syncMessage.setValue(result.getMessage());
        });
    }
    // accessing sync state
    public void assessBookingDataSyncState() {
        syncing.setValue(true);
        dataSyncRepository.assessBookingDataSyncState(this::applySyncAssessment);
    }

    public void markSyncStarted() {
        syncing.setValue(true);
        syncStatus.setValue(DataSyncStatus.ONLINE_SYNC_IN_PROGRESS);
        dataSyncRepository.markSyncStarted();
    }

    public void markLocalBookingDataStored() {
        dataSyncRepository.markLocalBookingDataStored();
        syncing.setValue(false);
        cachedBookingDataAvailable.setValue(true);
        syncCompleted.setValue(true);
        lastSuccessfulSyncAt.setValue(System.currentTimeMillis());
        syncStatus.setValue(DataSyncStatus.ONLINE_CACHE_FRESH);
        syncMessage.setValue("Booking data is ready.");
    }

    public void markNoDataFound() {
        dataSyncRepository.markNoDataFound();
        syncing.setValue(false);
        cachedBookingDataAvailable.setValue(false);
        syncStatus.setValue(DataSyncStatus.ONLINE_NO_DATA_FOUND);
        syncMessage.setValue("No booking data was returned by the backend.");
    }

    public void queueSyncForRetry(String message) {
        dataSyncRepository.queueSyncForRetry(message);
        syncing.setValue(false);
        syncStatus.setValue(DataSyncStatus.QUEUED_FOR_RETRY);
        syncMessage.setValue(message);
    }

    public void startSync() {
        syncing.setValue(true);
        syncCompleted.setValue(false);
        syncMessage.setValue("Syncing booking data...");
    }

    public void markCachedDataAvailable() {
        cachedBookingDataAvailable.setValue(true);
    }

    public void markSyncSuccess() {
        syncing.setValue(false);
        cachedBookingDataAvailable.setValue(true);
        syncCompleted.setValue(true);
        lastSuccessfulSyncAt.setValue(System.currentTimeMillis());
        syncMessage.setValue("Booking data is ready.");
    }

    public void markSyncFailure(String message) {
        syncing.setValue(false);
        syncCompleted.setValue(false);
        syncMessage.setValue(message);
    }

    public void clearSyncMessage() {
        syncMessage.setValue(null);
    }

    public void acknowledgeSyncCompleted() {
        syncCompleted.setValue(false);
    }

    @Override
    protected void onCleared() {
        dataSyncRepository.stopMonitoringSyncState(syncStateListener);
        dataSyncRepository.shutdown();
        super.onCleared();
    }

    private void applySyncAssessment(@NonNull SyncAssessmentResult result) {
        syncing.setValue(false);
        syncStatus.setValue(result.getStatus());
        userRole.setValue(result.getUserRole());
        cachedBookingDataAvailable.setValue(result.isLocalDataPresent());
        syncMessage.setValue(result.getMessage());
    }
}
