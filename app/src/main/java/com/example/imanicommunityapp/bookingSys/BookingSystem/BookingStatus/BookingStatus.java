package com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus;

public enum BookingStatus {
    IDLE,
    SELECTING_ROUTE,
    SELECTING_DROPOFF,
    SUBMITTING_BOOKING,
    PAYMENT_PENDING,
    PAYMENT_SUCCESSFUL,
    BOOKING_CONFIRMED,
    DRIVER_MATCHING,
    RIDE_ACTIVE,
    CANCELLING,
    CANCELLED,
    ERROR
}
