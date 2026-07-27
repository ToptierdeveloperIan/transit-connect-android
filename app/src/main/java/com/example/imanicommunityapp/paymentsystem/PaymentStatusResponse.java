package com.example.imanicommunityapp.paymentsystem;

import com.google.gson.annotations.SerializedName;

public class PaymentStatusResponse {

    @SerializedName("success")
    public boolean success;

    // Expected values: "pending", "completed", "failed", "cancelled"
    @SerializedName("status")
    public String status;

    @SerializedName("message")
    public String message;
}
