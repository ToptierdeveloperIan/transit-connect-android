package com.example.imanicommunityapp.bookingSys.BookingSystem.Models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Body for {@code POST bookings/checkout/} — light path (no Booking row).
 */
public class CheckoutRequest {

    @SerializedName("route_name")
    private final String routeName;

    @SerializedName("destination")
    private final String destination;

    @Nullable
    @SerializedName("promo_code")
    private final String promoCode;

    public CheckoutRequest(String routeName, String destination) {
        this(routeName, destination, null);
    }

    public CheckoutRequest(String routeName, String destination, @Nullable String promoCode) {
        this.routeName = routeName;
        this.destination = destination;
        this.promoCode = promoCode;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getDestination() {
        return destination;
    }

    @Nullable
    public String getPromoCode() {
        return promoCode;
    }
}
