package com.example.imanicommunityapp.auth.DataLayer;

import com.example.imanicommunityapp.GenericResponse;
import com.example.imanicommunityapp.auth.Model.LoginResponse;
import com.example.imanicommunityapp.auth.Model.PhoneLoginModel;
import com.example.imanicommunityapp.auth.Model.RefreshTokenRequest;
import com.example.imanicommunityapp.auth.Model.RefreshTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginTokenService {
    // STEP 1: Request OTP
    @POST("androidLogin/")
    Call<GenericResponse> requestOtp(@Body PhoneLoginModel data);
    //API endpoint
    @POST("token/")
    Call<LoginResponse> send_username(@Body PhoneLoginModel data);
    @POST("token/refresh/")
    Call<RefreshTokenResponse> refreshToken(
            @Body RefreshTokenRequest request
    );

}
