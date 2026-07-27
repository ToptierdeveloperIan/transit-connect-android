package com.example.imanicommunityapp.bookingSys;

public class CancelAvailabilityRequest {
    private String driverId;

    public CancelAvailabilityRequest(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
}
