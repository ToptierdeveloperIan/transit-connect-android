package com.example.imanicommunityapp;

import com.example.imanicommunityapp.auth.Model.VerifyCodeRequest;
import com.example.imanicommunityapp.supportProfile.UpdatePhoneRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;






public interface APIService {
    //API endpoint
    /** Relative to authRetrofitClient BASE_URL (.../api/). */
    @POST("register/")
    Call<Void> signup(@Body SignupData data);
    @POST("user/updatePhoneNumber")
    Call<Void> updatePhoneNumber(
            @Header("Authorization") String token,
            @Body UpdatePhoneRequest body
    );
    @POST("api/verify")
    Call<Void> verifyPhoneChange(
            @Header("Authorization") String token,
            @Body VerifyCodeRequest body
    );
}
