package com.example.imanicommunityapp.RandRsystem;

public class RedeemCodeValidationRequest {

    private final String code;

    public RedeemCodeValidationRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
