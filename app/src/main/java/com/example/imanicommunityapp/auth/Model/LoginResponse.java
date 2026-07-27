package com.example.imanicommunityapp.auth.Model;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String access;
    private String refresh;

    @SerializedName("is_driver")
    private boolean is_driver;

    @SerializedName("user_id")
    private String user_id;

    @SerializedName("first_name")
    private String firstName;

    @SerializedName("last_name")
    private String secondName;

    @SerializedName("phone_number")
    private String phoneNo;

    public String getAccessToken() { return access; }
    public String getRefreshToken() { return refresh; }
    public boolean isDriver() { return is_driver; }
    public String getUser_id() {
        return user_id;
    }
    public String getFirstName(){
        return firstName;
    }
    public String getSecondName(){
        return secondName;
    }
    public String getPhoneNo(){
        return phoneNo;
    }
}

