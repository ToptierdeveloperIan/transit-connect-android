package com.example.imanicommunityapp.Sync.SyncModels;
import com.google.gson.annotations.SerializedName;

public class InitialDataResponse {
    @SerializedName("destination")
    private String destination;

    @SerializedName("route")
    private String route;

    @SerializedName("coordinates")
    private CoordinatesModel coordinates;

    public String getDestination() {
        return destination;
    }

    public String getRoute() {
        return route;
    }

    public CoordinatesModel getCoordinates() {
        return coordinates;
    }
}
