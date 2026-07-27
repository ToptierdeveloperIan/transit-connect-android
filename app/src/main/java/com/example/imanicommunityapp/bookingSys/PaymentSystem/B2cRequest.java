package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import com.google.gson.annotations.SerializedName;

public class B2cRequest {

    @SerializedName("phone_number")
    private final String phoneNumber;

    @SerializedName("amount")
    private final String amount;

    @SerializedName("command_id")
    private final String commandId;

    @SerializedName("remarks")
    private final String remarks;

    @SerializedName("occasion")
    private final String occasion;

    public B2cRequest(String phoneNumber, String amount, String commandId, String remarks, String occasion) {
        this.phoneNumber = phoneNumber;
        this.amount = amount;
        this.commandId = commandId;
        this.remarks = remarks;
        this.occasion = occasion;
    }
}
