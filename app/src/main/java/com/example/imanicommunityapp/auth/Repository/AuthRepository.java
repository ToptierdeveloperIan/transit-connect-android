package com.example.imanicommunityapp.auth.Repository;

import android.content.Context;
import android.util.Log;

import com.example.imanicommunityapp.GenericResponse;
import com.example.imanicommunityapp.auth.DataLayer.LoginTokenService;
import com.example.imanicommunityapp.auth.Model.LoginResponse;
import com.example.imanicommunityapp.auth.Model.PhoneLoginModel;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final LoginTokenService api;
    private final TokenManager tokenManager;
    private final UserProfileRepository userProfileRepository;

    public AuthRepository(Context context) {
        Context appContext = context.getApplicationContext();

        // Plain hub client: OTP + token exchange must not carry Bearer / authenticator.
        authRetrofitClient.init(appContext);
        api = authRetrofitClient.getPlainClient().create(LoginTokenService.class);
        tokenManager = new TokenManager(appContext);
        userProfileRepository = new UserProfileRepository(appContext);
    }

    public void requestOtp(String phoneNumber, SimpleCallback callback) {
        PhoneLoginModel request = new PhoneLoginModel(phoneNumber);

        api.requestOtp(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to send OTP");
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void loginWithOtp(String phoneNumber, String otp, LoginCallback callback) {
        PhoneLoginModel request = new PhoneLoginModel(phoneNumber, otp);

        api.send_username(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse res = response.body();

                    tokenManager.saveAccessToken(res.getAccessToken());
                    Log.d("TOKEN", "Access" + res.getAccessToken());
                    tokenManager.saveRefreshToken(res.getRefreshToken());
                    Log.d("TOKEN", "REFRESH" + res.getRefreshToken());
                    tokenManager.saveUserID(res.getUser_id());
                    tokenManager.saveUserRole(res.isDriver() ? "driver" : "user");
                    userProfileRepository.saveUserProfile(res);

                    callback.onSuccess();
                } else {
                    callback.onError("Invalid OTP or user not found");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }
}
