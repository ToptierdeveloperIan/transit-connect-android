package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentsAPI {

    // STK Push (C2B)
    @POST("payments/stk/initiate/")
    Call<StkPushResponse> initiateStkPush(@Body StkPushRequest request);

    // STK Callback (from Safaricom)
    @POST("payments/stk/callback/")
    Call<Void> stkCallback(@Body Object callbackPayload);

    // B2C
    @POST("payments/b2c/initiate/")
    Call<B2cResponse> initiateB2c(@Body B2cRequest request);

    @POST("payments/b2c/result/")
    Call<Void> b2cResult(@Body Object resultPayload);

    @POST("payments/b2c/timeout/")
    Call<Void> b2cTimeout(@Body Object timeoutPayload);
}
