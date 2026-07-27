package com.example.imanicommunityapp.bookingSys;

import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;

public class BookingResponse {

    private boolean success;
    private String message;
    private int booking_id;
    private userCoordinates coordinates;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getBooking_id() {
        return booking_id;
    }

    public userCoordinates getCoordinates() {
        return coordinates;
    }
}
