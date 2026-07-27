package com.example.imanicommunityapp.auth.Model;

public class OTPVerifyModel {
    private String phone_number;
    private String otp;

    public OTPVerifyModel(String phone_number, String otp) {
        this.phone_number = phone_number;
        this.otp = otp;
    }
}
