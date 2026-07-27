package com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus;

import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;

public class BookingFlowState {
    private final BookingStatus status;
    private final String routeName;
    private final String dropOffName;
    private final String errorMessage;
    private final int bookingId;
    private final userCoordinates coordinates;

    public BookingFlowState() {
        this(BookingStatus.IDLE, null, null, null, -1, null);
    }

    public BookingFlowState(
            BookingStatus status,
            String routeName,
            String dropOffName,
            String errorMessage,
            int bookingId,
            userCoordinates coordinates
    ) {
        this.status = status;
        this.routeName = routeName;
        this.dropOffName = dropOffName;
        this.errorMessage = errorMessage;
        this.bookingId = bookingId;
        this.coordinates = coordinates;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getDropOffName() {
        return dropOffName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getBookingId() {
        return bookingId;
    }

    public userCoordinates getCoordinates() {
        return coordinates;
    }
}
