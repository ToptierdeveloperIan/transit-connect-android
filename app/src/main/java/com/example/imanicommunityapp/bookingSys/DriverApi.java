package com.example.imanicommunityapp.bookingSys;

import com.example.imanicommunityapp.GenericResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface DriverApi {

    // 1. Turn availability ON
    @POST("driver/set_availability/")
    Call<GenericResponse> setAvailability(
            @Body AvailabilityRequest body
    );

    // 2. Start trip (come online + share location)
    @POST("driver/StartTrip/")
    Call<GenericResponse> startTrip(
            @Body ComeOnlineRequest request
    );

    // 3. Cancel availability (only allowed if no bookings)
    @POST("driver/cancel_availability/")
    Call<GenericResponse> cancelAvailability(
            @Body CancelAvailabilityRequest body
    );

    // 4. End trip
    @POST("driver/EndTrip/")
    Call<Void> EndTrip(
            @Body EndTripModel request
    );
}
