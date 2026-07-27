package com.example.imanicommunityapp.auth.Model;

import com.google.gson.annotations.SerializedName;

public class PhoneLoginModel {

    @SerializedName("phone_number")
    private final String phoneNumber;

    @SerializedName("otp")
    private final String otp;

    // Constructor for REQUEST OTP
    public PhoneLoginModel(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.otp = null;
    }

    // Constructor for VERIFY OTP
    public PhoneLoginModel(String phoneNumber, String otp) {
        this.phoneNumber = phoneNumber;
        this.otp = otp;
    }
}
