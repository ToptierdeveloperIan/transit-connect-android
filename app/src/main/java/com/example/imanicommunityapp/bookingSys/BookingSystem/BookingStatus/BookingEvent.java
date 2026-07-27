package com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;

public class BookingEvent {
    public enum Type {
        OPEN_BOOKING,
        ROUTE_SELECTED,
        DROPOFF_SELECTED,
        SUBMIT_BOOKING,
        BOOKING_SUCCESS,
        BOOKING_FAILURE,
        CANCEL_REQUESTED,
        CANCEL_SUCCESS,
        RESET
    }

    private final Type type;
    @Nullable
    private final String routeName;
    @Nullable
    private final String dropOffName;
    @Nullable
    private final String errorMessage;
    private final int bookingId;
    @Nullable
    private final userCoordinates coordinates;

    private BookingEvent(
            Type type,
            @Nullable String routeName,
            @Nullable String dropOffName,
            @Nullable String errorMessage,
            int bookingId,
            @Nullable userCoordinates coordinates
    ) {
        this.type = type;
        this.routeName = routeName;
        this.dropOffName = dropOffName;
        this.errorMessage = errorMessage;
        this.bookingId = bookingId;
        this.coordinates = coordinates;
    }

    public static BookingEvent openBooking() {
        return new BookingEvent(Type.OPEN_BOOKING, null, null, null, -1, null);
    }

    public static BookingEvent routeSelected(String routeName) {
        return new BookingEvent(Type.ROUTE_SELECTED, routeName, null, null, -1, null);
    }

    public static BookingEvent dropoffSelected(String dropOffName) {
        return new BookingEvent(Type.DROPOFF_SELECTED, null, dropOffName, null, -1, null);
    }

    public static BookingEvent submitBooking() {
        return new BookingEvent(Type.SUBMIT_BOOKING, null, null, null, -1, null);
    }

    public static BookingEvent bookingSuccess(int bookingId, @Nullable userCoordinates coordinates) {
        return new BookingEvent(Type.BOOKING_SUCCESS, null, null, null, bookingId, coordinates);
    }

    public static BookingEvent bookingFailure(String message) {
        return new BookingEvent(Type.BOOKING_FAILURE, null, null, message, -1, null);
    }

    public static BookingEvent cancelRequested() {
        return new BookingEvent(Type.CANCEL_REQUESTED, null, null, null, -1, null);
    }

    public static BookingEvent cancelSuccess() {
        return new BookingEvent(Type.CANCEL_SUCCESS, null, null, null, -1, null);
    }

    public static BookingEvent reset() {
        return new BookingEvent(Type.RESET, null, null, null, -1, null);
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public String getRouteName() {
        return routeName;
    }

    @Nullable
    public String getDropOffName() {
        return dropOffName;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public int getBookingId() {
        return bookingId;
    }

    @Nullable
    public userCoordinates getCoordinates() {
        return coordinates;
    }
}
