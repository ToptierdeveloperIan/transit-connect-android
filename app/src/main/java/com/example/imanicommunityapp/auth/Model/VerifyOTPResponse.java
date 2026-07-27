package com.example.imanicommunityapp.auth.Model;

public class VerifyOTPResponse {
    private boolean success;
    private String phone_number;

    public boolean isSuccess() { return success; }
    public String getPhoneNumber() { return phone_number; }
}
