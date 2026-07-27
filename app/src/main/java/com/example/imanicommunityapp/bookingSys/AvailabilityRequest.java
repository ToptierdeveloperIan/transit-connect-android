package com.example.imanicommunityapp.bookingSys;

public class AvailabilityRequest {
    public boolean available;
    public String driver_id;
    public AvailabilityRequest(boolean available, String driver_id) {
        this.available = available;
        this.driver_id = driver_id;
    }
}
