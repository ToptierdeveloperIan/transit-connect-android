package com.example.imanicommunityapp.bookingSys.BookingSystem.Models;

import com.google.gson.annotations.SerializedName;

public class BookingModel {
    @SerializedName("user_id")
    private String id;
    @SerializedName("route_name")
    private String RouteName;
    @SerializedName("destination")
    private String dropOff;

    public BookingModel(String RouteName, String dropoff_name,String id){
        this.RouteName=RouteName;
        this.dropOff=dropoff_name;
        this.id=id;
    }

    public String getDropOff() {
        return dropOff;
    }

    public String getRouteName() {
        return RouteName;
    }

    public String getUserId() {
        return id;
    }
}
