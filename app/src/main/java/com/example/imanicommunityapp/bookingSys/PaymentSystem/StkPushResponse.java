package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import com.google.gson.annotations.SerializedName;

public class StkPushResponse {
    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("checkout_request_id")
    public String checkoutRequestId;

    @SerializedName("error_code")
    public String errorCode;
}
