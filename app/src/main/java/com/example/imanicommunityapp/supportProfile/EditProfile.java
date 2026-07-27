package com.example.imanicommunityapp.supportProfile;

import com.example.imanicommunityapp.auth.Model.OTPVerifyModel;
import com.example.imanicommunityapp.auth.Model.PhoneLoginModel;
import com.example.imanicommunityapp.auth.Model.VerifyOTPResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface EditProfile {
    @POST("api/editingname/")
    Call<Void> editName(
            @Header("Authorization") String token,
            @Body editingNameModel editName);

    @POST("api/editingPhoneNo/")
    Call<Void> editPhoneNo(
            @Header("Authorization") String token,
            @Body PhoneLoginModel phoneno);

    @POST("api/editingEmail/")
    Call<Void> editEmail(
            @Header("Authorization") String token,
            @Body editingNameModel editName);
    // Step 2 — verify OTP
    @POST("api/profile/verify-phone-otp/")
    Call<VerifyOTPResponse> verifyPhoneOTP(
            @Header("Authorization") String token,
            @Body OTPVerifyModel otpRequest);
}
