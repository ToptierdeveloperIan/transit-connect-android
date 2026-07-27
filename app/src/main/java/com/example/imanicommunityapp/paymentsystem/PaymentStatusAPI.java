package com.example.imanicommunityapp.paymentsystem;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PaymentStatusAPI {

    // Poll for payment confirmation after STK push is sent.
    // Backend receives the Safaricom callback and stores the result;
    // this endpoint returns the current status for a given checkoutRequestId.
    @GET("payments/stk/status/{checkoutRequestId}/")
    Call<PaymentStatusResponse> checkPaymentStatus(
            @Path("checkoutRequestId") String checkoutRequestId
    );
}
