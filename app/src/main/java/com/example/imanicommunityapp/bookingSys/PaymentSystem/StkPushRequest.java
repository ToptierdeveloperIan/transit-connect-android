package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import com.google.gson.annotations.SerializedName;

public class StkPushRequest {
    @SerializedName("amount")
    private final String amount;

    @SerializedName("booking_id")
    private final Integer bookingId;

    @SerializedName("account_reference")
    private final String accountReference;

    @SerializedName("transaction_desc")
    private final String transactionDesc;

    public StkPushRequest(String amount, Integer bookingId, String accountReference, String transactionDesc) {
        this.amount = amount;
        this.bookingId = bookingId;
        this.accountReference = accountReference;
        this.transactionDesc = transactionDesc;
    }
}
