package com.example.imanicommunityapp.settings;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.imanicommunityapp.Network.ConnectivityChecker;
import com.example.imanicommunityapp.auth.DataLayer.UserProfileRoomDb;
import com.example.imanicommunityapp.auth.Repository.UserProfileRepository;
import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;
import com.example.imanicommunityapp.settings.sync.ProfileNameQueue;

import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Production profile settings access.
 *
 * <p><b>Names:</b> online PATCH immediately; offline optimistic Room + queue event.
 * <p><b>Phone:</b> online OTP request/confirm only — never queued offline.
 * <p><b>Room:</b> account phone is updated only after confirm success.
 *
 * Server is source of truth; local is a projection with optional pending name.
 */
public class SettingsRepository {

    private static final String TAG = "SettingsRepository";

    public interface SimpleCallback {
        void onSuccess(@Nullable String message);

        void onError(@NonNull String message);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(@NonNull String message);
    }

    private final Context appContext;
    private final SettingsApi api;
    private final UserProfileRepository userProfileRepository;
    private final ConnectivityChecker connectivityChecker;
    private final ProfileNameQueue nameQueue;
    private final Handler main = new Handler(Looper.getMainLooper());

    public SettingsRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = authRetrofitClient.getClient(appContext).create(SettingsApi.class);
        this.userProfileRepository = new UserProfileRepository(appContext);
        this.connectivityChecker = new ConnectivityChecker(appContext);
        this.nameQueue = ProfileNameQueue.getInstance(appContext);
    }

    /** Pull authoritative snapshot and merge into Room (respects pending-name rules). */
    public void refreshProfile(@NonNull SimpleCallback callback) {
        api.getProfile().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call,
                                   Response<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> response) {
                SettingsModels.Envelope<SettingsModels.ProfileSnapshot> body = response.body();
                if (!response.isSuccessful() || body == null || !Boolean.TRUE.equals(body.success) || body.data == null) {
                    postError(callback, messageOr(body, "Failed to load profile"));
                    return;
                }
                userProfileRepository.applyServerProfile(body.data, () ->
                        postSuccess(callback, body.message));
            }

            @Override
            public void onFailure(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call, Throwable t) {
                postError(callback, t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    /**
     * Save names. If offline, applies optimistic local state and enqueues PROFILE_NAME_UPDATE.
     * If online, PATCHes immediately with mutation_id + base_version.
     */
    public void updateName(@NonNull String firstName, @NonNull String secondName, @NonNull SimpleCallback callback) {
        String mutationId = UUID.randomUUID().toString();
        userProfileRepository.getUserProfile(profile -> {
            int baseVersion = profile != null ? profile.profileVersion : 0;

            if (!connectivityChecker.hasInternetConnection()) {
                // --- Offline path: local projection + durable queue ---
                userProfileRepository.applyOfflineNamePending(
                        firstName, secondName, mutationId, baseVersion,
                        () -> {
                            nameQueue.enqueueNameUpdate(firstName, secondName, mutationId, baseVersion);
                            postSuccess(callback, "Saved offline. Will sync when you are online.");
                        });
                return;
            }

            // --- Online path ---
            SettingsModels.UpdateNameRequest req =
                    new SettingsModels.UpdateNameRequest(firstName, secondName, mutationId, baseVersion);
            api.updateName(req).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call,
                                       Response<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> response) {
                    SettingsModels.Envelope<SettingsModels.ProfileSnapshot> body = response.body();
                    if (response.code() == 409) {
                        // Server wins: rehydrate then surface conflict.
                        refreshProfile(new SimpleCallback() {
                            @Override
                            public void onSuccess(@Nullable String message) {
                                postError(callback, "Name was updated on the server. Showing latest profile.");
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                postError(callback, "Version conflict. Pull to refresh profile.");
                            }
                        });
                        return;
                    }
                    if (!response.isSuccessful() || body == null || !Boolean.TRUE.equals(body.success) || body.data == null) {
                        postError(callback, messageOr(body, "Name update failed"));
                        return;
                    }
                    userProfileRepository.applyServerProfile(body.data, () ->
                            postSuccess(callback, body.message != null ? body.message : "Name updated"));
                }

                @Override
                public void onFailure(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call, Throwable t) {
                    // Network failure mid-flight: queue so we do not lose the edit.
                    userProfileRepository.applyOfflineNamePending(
                            firstName, secondName, mutationId, baseVersion,
                            () -> {
                                nameQueue.enqueueNameUpdate(firstName, secondName, mutationId, baseVersion);
                                postSuccess(callback, "Saved offline. Will sync when connection is stable.");
                            });
                }
            });
        });
    }

    /**
     * Phone step 1 — requires internet. Does not change account phone in Room.
     */
    public void requestPhoneChange(@NonNull String newPhone, @NonNull DataCallback<SettingsModels.PhoneRequestResult> callback) {
        if (!connectivityChecker.hasInternetConnection()) {
            postDataError(callback, "Connect to the internet to change your phone number.");
            return;
        }
        String mutationId = UUID.randomUUID().toString();
        api.requestPhoneChange(new SettingsModels.PhoneRequestBody(newPhone, mutationId))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<SettingsModels.Envelope<SettingsModels.PhoneRequestResult>> call,
                                           Response<SettingsModels.Envelope<SettingsModels.PhoneRequestResult>> response) {
                        SettingsModels.Envelope<SettingsModels.PhoneRequestResult> body = response.body();
                        if (!response.isSuccessful() || body == null || !Boolean.TRUE.equals(body.success) || body.data == null) {
                            postDataError(callback, messageOr(body, "Could not send verification code"));
                            return;
                        }
                        // Draft only — not account phone.
                        userProfileRepository.setPhonePendingVerification(newPhone, () ->
                                main.post(() -> callback.onSuccess(body.data)));
                    }

                    @Override
                    public void onFailure(Call<SettingsModels.Envelope<SettingsModels.PhoneRequestResult>> call, Throwable t) {
                        postDataError(callback, t.getMessage() != null ? t.getMessage() : "Network error");
                    }
                });
    }

    /**
     * Phone step 2 — OTP confirm. Room phone_no updated only on success.
     */
    public void confirmPhoneChange(
            @NonNull String challengeId,
            @NonNull String otp,
            @NonNull String mutationId,
            @NonNull SimpleCallback callback
    ) {
        if (!connectivityChecker.hasInternetConnection()) {
            postError(callback, "Connect to the internet to verify your new number.");
            return;
        }
        api.confirmPhoneChange(new SettingsModels.PhoneConfirmBody(challengeId, otp, mutationId))
                .enqueue(new Callback<>() {
                    @Override
                    public void onResponse(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call,
                                           Response<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> response) {
                        SettingsModels.Envelope<SettingsModels.ProfileSnapshot> body = response.body();
                        if (!response.isSuccessful() || body == null || !Boolean.TRUE.equals(body.success) || body.data == null) {
                            postError(callback, messageOr(body, "Verification failed"));
                            return;
                        }
                        userProfileRepository.applyServerProfile(body.data, () ->
                                postSuccess(callback, body.message != null ? body.message : "Phone updated"));
                    }

                    @Override
                    public void onFailure(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call, Throwable t) {
                        postError(callback, t.getMessage() != null ? t.getMessage() : "Network error");
                    }
                });
    }

    /** Exposed for queue processor (same API, no offline re-queue). */
    public void pushNameMutation(
            @NonNull String firstName,
            @NonNull String secondName,
            @NonNull String mutationId,
            int baseVersion,
            @NonNull SimpleCallback callback
    ) {
        SettingsModels.UpdateNameRequest req =
                new SettingsModels.UpdateNameRequest(firstName, secondName, mutationId, baseVersion);
        api.updateName(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call,
                                   Response<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> response) {
                SettingsModels.Envelope<SettingsModels.ProfileSnapshot> body = response.body();
                if (response.code() == 409) {
                    // Server wins: drop pending, apply server snapshot.
                    refreshProfile(callback);
                    return;
                }
                if (!response.isSuccessful() || body == null || !Boolean.TRUE.equals(body.success) || body.data == null) {
                    postError(callback, messageOr(body, "Sync name failed"));
                    return;
                }
                userProfileRepository.applyServerProfile(body.data, () ->
                        postSuccess(callback, "Name synced"));
            }

            @Override
            public void onFailure(Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> call, Throwable t) {
                postError(callback, t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    private static String messageOr(SettingsModels.Envelope<?> body, String fallback) {
        if (body != null && body.message != null && !body.message.isEmpty()) {
            return body.message;
        }
        return fallback;
    }

    private void postSuccess(SimpleCallback callback, @Nullable String message) {
        main.post(() -> callback.onSuccess(message));
    }

    private void postError(SimpleCallback callback, @NonNull String message) {
        Log.w(TAG, message);
        main.post(() -> callback.onError(message));
    }

    private <T> void postDataError(DataCallback<T> callback, @NonNull String message) {
        Log.w(TAG, message);
        main.post(() -> callback.onError(message));
    }
}
