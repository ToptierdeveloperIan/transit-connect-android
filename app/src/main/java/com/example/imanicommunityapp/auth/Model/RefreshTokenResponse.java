package com.example.imanicommunityapp.auth.Model;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenResponse {
    @SerializedName("access")
    private String accessToken;
    @SerializedName("refresh")
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}

