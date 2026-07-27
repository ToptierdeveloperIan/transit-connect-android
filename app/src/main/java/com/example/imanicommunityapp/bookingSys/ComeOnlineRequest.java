package com.example.imanicommunityapp.bookingSys;

import com.google.gson.annotations.SerializedName;

public class ComeOnlineRequest {
    @SerializedName("user_id")
    private final String User_ID;

    public ComeOnlineRequest(String User_ID) { this.User_ID=User_ID; }
}
