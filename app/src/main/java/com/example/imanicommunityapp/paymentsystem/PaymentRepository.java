package com.example.imanicommunityapp.paymentsystem;

import android.content.Context;

import com.example.imanicommunityapp.RetrofitClient;
import com.example.imanicommunityapp.bookingSys.PaymentSystem.PaymentsAPI;
import com.example.imanicommunityapp.bookingSys.PaymentSystem.StkPushRequest;
import com.example.imanicommunityapp.bookingSys.PaymentSystem.StkPushResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PaymentRepository {

    private final PaymentsAPI paymentsAPI;
    private final PaymentStatusAPI paymentStatusAPI;

    public PaymentRepository(Context context) {
        Retrofit retrofit = RetrofitClient.getClient(context.getApplicationContext());
        paymentsAPI = retrofit.create(PaymentsAPI.class);
        paymentStatusAPI = retrofit.create(PaymentStatusAPI.class);
    }

    public void initiateStkPush(String amount, int bookingId, PaymentCallback<StkPushResponse> callback) {
        StkPushRequest request = new StkPushRequest(amount, bookingId, "ImaniRide", "Ride Payment");
        paymentsAPI.initiateStkPush(request).enqueue(new Callback<StkPushResponse>() {
            @Override
            public void onResponse(Call<StkPushResponse> call, Response<StkPushResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to initiate payment. Please try again.");
                }
            }

            @Override
            public void onFailure(Call<StkPushResponse> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void checkPaymentStatus(String checkoutRequestId, PaymentCallback<PaymentStatusResponse> callback) {
        paymentStatusAPI.checkPaymentStatus(checkoutRequestId).enqueue(new Callback<PaymentStatusResponse>() {
            @Override
            public void onResponse(Call<PaymentStatusResponse> call, Response<PaymentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to check payment status");
                }
            }

            @Override
            public void onFailure(Call<PaymentStatusResponse> call, Throwable t) {
                callback.onError(t.getMessage() != null ? t.getMessage() : "Network error");
            }
        });
    }

    public interface PaymentCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }
}
