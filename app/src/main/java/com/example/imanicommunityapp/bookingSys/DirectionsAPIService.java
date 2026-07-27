package com.example.imanicommunityapp.bookingSys;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DirectionsAPIService {
    @GET("directions/json")
    Call<directionsAPIResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("key") String apiKey
    );
}
