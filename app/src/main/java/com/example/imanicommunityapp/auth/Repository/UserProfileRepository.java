package com.example.imanicommunityapp.auth.Repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.auth.DataLayer.UserProfileRoomDb;
import com.example.imanicommunityapp.auth.Model.LoginResponse;
import com.example.imanicommunityapp.settings.SettingsModels;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local profile projection (Room).
 *
 * <p>Rules (see docs/PROFILE_SETTINGS_SYNC.md):
 * <ul>
 *   <li>Account phone is only written from server-confirmed snapshots.</li>
 *   <li>Offline name edits set pending flags; rehydrate applies server-wins merge.</li>
 *   <li>{@code profile_version} tracks ResourceVersion for optimistic concurrency.</li>
 * </ul>
 */
public class UserProfileRepository {

    private static final String TAG = "UserProfileRepository";

    public interface ProfileCallback {
        void onProfileLoaded(@Nullable UserProfileRoomDb.UserProfileEntity profile);
    }

    public interface CompletionCallback {
        void onComplete();
    }

    private final UserProfileRoomDb userProfileRoomDb;
    private final ExecutorService databaseExecutor;
    private final Handler mainHandler;

    public UserProfileRepository(Context context) {
        Context appContext = context.getApplicationContext();
        userProfileRoomDb = UserProfileRoomDb.getInstance(appContext);
        databaseExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void saveUserProfile(LoginResponse response) {
        saveUserProfile(new UserProfileRoomDb.UserProfileEntity(
                response.getUser_id(),
                response.isDriver() ? "driver" : "user",
                response.getFirstName(),
                response.getSecondName(),
                response.getPhoneNo()
        ));
    }

    public void saveUserProfile(UserProfileRoomDb.UserProfileEntity profile) {
        databaseExecutor.execute(() -> userProfileRoomDb.userProfileDao().saveUserProfile(profile));
    }

    public void getUserProfile(ProfileCallback callback) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            mainHandler.post(() -> callback.onProfileLoaded(profile));
        });
    }

    public void updateUserName(String firstName, String secondName, @Nullable CompletionCallback callback) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            if (profile != null) {
                profile.firstName = firstName;
                profile.secondName = secondName;
                userProfileRoomDb.userProfileDao().saveUserProfile(profile);
            }
            notifyCompletion(callback);
        });
    }

    public void updateUserPhone(String phoneNumber, @Nullable CompletionCallback callback) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            if (profile != null) {
                profile.phoneNo = phoneNumber;
                profile.phonePendingVerification = null;
                userProfileRoomDb.userProfileDao().saveUserProfile(profile);
            }
            notifyCompletion(callback);
        });
    }

    /**
     * Optimistic offline name: UI shows new names, pending flags set for queue + rehydrate merge.
     */
    public void applyOfflineNamePending(
            String firstName,
            String secondName,
            String mutationId,
            int baseVersion,
            @Nullable CompletionCallback callback
    ) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            if (profile != null) {
                profile.firstName = firstName;
                profile.secondName = secondName;
                profile.pendingNameMutation = true;
                profile.pendingMutationId = mutationId;
                profile.pendingBaseVersion = baseVersion;
                userProfileRoomDb.userProfileDao().saveUserProfile(profile);
            }
            notifyCompletion(callback);
        });
    }

    /** Draft phone while OTP is outstanding — never used as login identity. */
    public void setPhonePendingVerification(String draftPhone, @Nullable CompletionCallback callback) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            if (profile != null) {
                profile.phonePendingVerification = draftPhone;
                userProfileRoomDb.userProfileDao().saveUserProfile(profile);
            }
            notifyCompletion(callback);
        });
    }

    /**
     * Apply server snapshot with pending-name merge rules (server wins if version advanced).
     */
    public void applyServerProfile(
            SettingsModels.ProfileSnapshot snapshot,
            @Nullable CompletionCallback callback
    ) {
        databaseExecutor.execute(() -> {
            UserProfileRoomDb.UserProfileEntity profile = userProfileRoomDb.userProfileDao().getUserProfile();
            int serverVersion = snapshot.profileVersion != null ? snapshot.profileVersion : 0;

            if (profile == null) {
                String role = Boolean.TRUE.equals(snapshot.isDriver) ? "driver" : "user";
                String uid = snapshot.userId != null ? String.valueOf(snapshot.userId) : "";
                profile = new UserProfileRoomDb.UserProfileEntity(
                        uid,
                        role,
                        snapshot.firstName,
                        snapshot.secondName,
                        snapshot.phoneNumber
                );
            }

            // --- Pending name merge ---
            if (profile.pendingNameMutation) {
                if (serverVersion > profile.pendingBaseVersion) {
                    // Server moved ahead (other device / conflict): drop pending, take server names.
                    Log.i(TAG, "Dropping pending name; server version advanced");
                    profile.firstName = snapshot.firstName;
                    profile.secondName = snapshot.secondName;
                    profile.pendingNameMutation = false;
                    profile.pendingMutationId = null;
                    profile.pendingBaseVersion = 0;
                } else if (serverVersion == profile.pendingBaseVersion) {
                    // Still waiting to push; keep optimistic local names.
                    Log.d(TAG, "Keeping pending name; server version unchanged");
                } else {
                    // Unexpected: server version behind local base — still take server as truth for identity.
                    profile.firstName = snapshot.firstName;
                    profile.secondName = snapshot.secondName;
                    profile.pendingNameMutation = false;
                    profile.pendingMutationId = null;
                }
            } else {
                profile.firstName = snapshot.firstName;
                profile.secondName = snapshot.secondName;
            }

            // Phone: always from server when present (never from offline draft).
            if (snapshot.phoneNumber != null) {
                profile.phoneNo = snapshot.phoneNumber;
            }
            profile.phonePendingVerification = null;
            profile.profileVersion = serverVersion;
            if (snapshot.userId != null) {
                profile.userId = String.valueOf(snapshot.userId);
            }
            if (snapshot.isDriver != null) {
                profile.userRole = snapshot.isDriver ? "driver" : "user";
            }

            userProfileRoomDb.userProfileDao().saveUserProfile(profile);
            notifyCompletion(callback);
        });
    }

    public void clearUserProfile(@Nullable CompletionCallback callback) {
        databaseExecutor.execute(() -> {
            userProfileRoomDb.userProfileDao().clearUserProfile();
            notifyCompletion(callback);
        });
    }

    private void notifyCompletion(@Nullable CompletionCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onComplete);
        }
    }
}
