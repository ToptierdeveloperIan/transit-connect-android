package com.example.imanicommunityapp.Sync;

import androidx.annotation.NonNull;

//CallBack for Assessing Sync
public interface RepositoryCallback {
    void onResult(@NonNull SyncAssessmentResult result);
}
