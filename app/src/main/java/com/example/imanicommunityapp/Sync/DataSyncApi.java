package com.example.imanicommunityapp.Sync;

import com.example.imanicommunityapp.GenericResponse;
import com.example.imanicommunityapp.Sync.SyncModels.InitialDataResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface DataSyncApi {

    @GET("DataSync")
    //This is to ensure that data is not stale.
    Call<GenericResponse> getDataSync();

    @GET("/api/initial-data/")
    //Data needed for inital app setup
    Call<InitialDataResponse> getInitialData();

    @POST("DataSync")
    // If the data has been found to be stale then we post what we have locally.
    Call<GenericResponse> postDataSync(@Body Map<String, Object> payload);
}
