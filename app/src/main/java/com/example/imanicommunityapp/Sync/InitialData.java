package com.example.imanicommunityapp.Sync;

import android.util.Log;

import com.example.imanicommunityapp.Sync.SyncModels.InitialDataResponse;
import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class InitialData {

    // Singleton Retrofit instance
    private final Retrofit retrofit = authRetrofitClient.getPlainClient();

    // API interface
    private final DataSyncApi dataSyncApi = retrofit.create(DataSyncApi.class);

    // Fetch initial data from the backend
    public void fetchInitialData() {
        dataSyncApi.getInitialData().enqueue(new Callback<InitialDataResponse>() {

            @Override
            public void onResponse(Call<InitialDataResponse> call,
                                   Response<InitialDataResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    InitialDataResponse initialData = response.body();

                    // TODO: Save data to Room/SQLite
                    Log.d("InitialData", "Data received successfully");

                } else {
                    Log.e("InitialData", "Request failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<InitialDataResponse> call, Throwable t) {
                Log.e("InitialData", "Network error", t);
            }
        });
    }
}