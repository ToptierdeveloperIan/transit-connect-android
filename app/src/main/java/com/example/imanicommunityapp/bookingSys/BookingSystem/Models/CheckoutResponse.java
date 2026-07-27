package com.example.imanicommunityapp.bookingSys.BookingSystem.Models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Response for {@code POST bookings/checkout/}.
 *
 * <p>{@code booking_id} is null — no canonical booking yet.
 * Pay using {@code quote_id} / coordinates.discounted_fare when present.
 */
public class CheckoutResponse {

    private boolean success;

    @Nullable
    private String message;

    @Nullable
    private String error;

    @SerializedName("route_name")
    @Nullable
    private String routeName;

    @Nullable
    private String stop;

    @Nullable
    private String destination;

    @Nullable
    private userCoordinates coordinates;

    @SerializedName("quote_id")
    @Nullable
    private String quoteId;

    /**
     * Always null on checkout success (no Booking row).
     * Gson may omit or send null.
     */
    @SerializedName("booking_id")
    @Nullable
    private Integer bookingId;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getError() {
        return error;
    }

    @Nullable
    public String getRouteName() {
        return routeName;
    }

    @Nullable
    public String getStop() {
        return stop != null ? stop : destination;
    }

    @Nullable
    public userCoordinates getCoordinates() {
        return coordinates;
    }

    @Nullable
    public String getQuoteId() {
        if (quoteId != null) {
            return quoteId;
        }
        if (coordinates != null && coordinates.getQuoteId() != null) {
            return coordinates.getQuoteId();
        }
        return null;
    }

    @Nullable
    public Integer getBookingId() {
        return bookingId;
    }
}
