package com.example.imanicommunityapp.bookingSys;

public enum DriverState {
    OFFLINE,          // Driver not available
    AVAILABLE,        // Driver available (can cancel OR start trip)
    TRIP_IN_PROGRESS  // Driver actively on trip
}

