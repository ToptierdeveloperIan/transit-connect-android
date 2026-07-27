package com.example.imanicommunityapp.bookingSys.PaymentSystem;

import com.google.gson.annotations.SerializedName;

public class B2cResponse {

    @SerializedName("success")
    public boolean success;

    @SerializedName("message")
    public String message;

    @SerializedName("originator_conversation_id")
    public String originatorConversationId;

    @SerializedName("conversation_id")
    public String conversationId;

    @SerializedName("error_code")
    public String errorCode;
}
