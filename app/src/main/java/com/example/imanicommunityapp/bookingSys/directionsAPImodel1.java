package com.example.imanicommunityapp.bookingSys;

public class directionsAPImodel1 {
    private final double startLat;
    private final double startLng;
    private final double endLat;
    private final double endLng;
    private final String apiKey;

    public directionsAPImodel1(double startLat, double startLng, double endLat, double endLng, String apiKey) {
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.apiKey = apiKey;
    }

    public String getOrigin() {
        return startLat + "," + startLng;
    }

    public String getDestination() {
        return endLat + "," + endLng;
    }

    public String getApiKey() {
        return apiKey;
    }
}


