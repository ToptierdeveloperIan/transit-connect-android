package com.example.imanicommunityapp.RandRsystem;

import androidx.annotation.Nullable;

public class RedeemCodeValidationResponse {

    private boolean success;
    @Nullable
    private String message;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }
}
