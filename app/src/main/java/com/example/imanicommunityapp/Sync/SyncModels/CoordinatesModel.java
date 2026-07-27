package com.example.imanicommunityapp.Sync.SyncModels;

import com.google.gson.annotations.SerializedName;

public class CoordinatesModel {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
