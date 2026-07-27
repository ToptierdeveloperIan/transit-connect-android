package com.example.imanicommunityapp;
import com.google.gson.annotations.SerializedName;

public class SignupData {
    @SerializedName("Username")
    private String username;

    //object creation
    public SignupData(String username){
        this.username=username;
    }

    // Getter method
    public String getUsername(){
        return username;
    }


}
