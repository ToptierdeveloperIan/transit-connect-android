package com.example.imanicommunityapp.bookingSys;

import com.example.imanicommunityapp.GenericResponse;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.BookingModel;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutRequest;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BookingInterface {

    /**
     * Light checkout: validate route/stop, return coords + fares + quote.
     * Does not create a server Booking.
     */
    @POST("bookings/checkout/")
    Call<CheckoutResponse> checkout(@Body CheckoutRequest request);

    /** Legacy canonical match + Booking.create — prefer {@link #checkout}. */
    @POST("bookings/create/")
    Call<BookingResponse> createBooking(@Body BookingModel booking);

    @GET("api/bookings/active/")
    Call<RideStatusResponse> getActiveBooking(@Header("Authorization") String token);

    @POST("api/bookings/cancel/")
    Call<CancelResponse> cancelBooking(@Body CancelRequest request);

    @POST("booking/{id}/update-status")
    @FormUrlEncoded
    Call<GenericResponse> updateRideStatus(
            @Path("id") String bookingId,
            @Field("status") String status
    );
}
