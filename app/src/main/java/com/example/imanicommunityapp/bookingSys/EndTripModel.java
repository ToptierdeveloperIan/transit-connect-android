package com.example.imanicommunityapp.bookingSys;

import com.google.gson.annotations.SerializedName;

public class EndTripModel {
    @SerializedName("user_id")
    private final String User_id;

    public EndTripModel(String User_id){
        this.User_id=User_id;
    }
}
