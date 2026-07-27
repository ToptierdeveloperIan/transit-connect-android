package com.example.imanicommunityapp.RandRsystem;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RedeemCodeApi {

    @POST("RedeemCodeValidation")
    Call<RedeemCodeValidationResponse> validateRedeemCode(
            @Body RedeemCodeValidationRequest request
    );
}
