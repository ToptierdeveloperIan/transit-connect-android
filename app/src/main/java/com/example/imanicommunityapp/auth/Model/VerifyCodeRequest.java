package com.example.imanicommunityapp.auth.Model;

public class VerifyCodeRequest {
    private String OTP;

    public VerifyCodeRequest(String OTP){
        this.OTP=OTP;
    }
    public String getVerifiationCode(){
        return this.OTP;
    }
}
