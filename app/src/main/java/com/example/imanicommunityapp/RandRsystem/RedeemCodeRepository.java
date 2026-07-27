package com.example.imanicommunityapp.RandRsystem;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.imanicommunityapp.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RedeemCodeRepository {

    private final RedeemCodeApi redeemCodeApi;

    public RedeemCodeRepository(@NonNull Context context) {
        redeemCodeApi = RetrofitClient
                .getClient(context.getApplicationContext())
                .create(RedeemCodeApi.class);
    }

    public void validateRedeemCode(
            @NonNull String code,
            @NonNull RedeemCodeCallback callback
    ) {
        redeemCodeApi.validateRedeemCode(new RedeemCodeValidationRequest(code))
                .enqueue(new Callback<RedeemCodeValidationResponse>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<RedeemCodeValidationResponse> call,
                            @NonNull Response<RedeemCodeValidationResponse> response
                    ) {
                        if (!response.isSuccessful()) {
                            callback.onError("We could not validate your code right now. Please try again later.");
                            return;
                        }

                        RedeemCodeValidationResponse body = response.body();
                        if (body == null) {
                            callback.onError("The server returned an empty response. Please try again later.");
                            return;
                        }

                        if (body.isSuccess()) {
                            callback.onSuccess(body.getMessage() != null
                                    ? body.getMessage()
                                    : "Code validated successfully.");
                            return;
                        }

                        callback.onError(body.getMessage() != null
                                ? body.getMessage()
                                : "That code could not be redeemed.");
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<RedeemCodeValidationResponse> call,
                            @NonNull Throwable t
                    ) {
                        callback.onError("We could not reach the server. Please try again later.");
                    }
                });
    }

    public interface RedeemCodeCallback {
        void onSuccess(@NonNull String message);
        void onError(@NonNull String message);
    }
}
