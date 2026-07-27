package com.example.imanicommunityapp;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;

import retrofit2.Retrofit;

/**
 * App-wide facade for authenticated backend API access.
 * Delegates to {@link authRetrofitClient} so all /api/ traffic shares one hub.
 */
public final class RetrofitClient {

    private RetrofitClient() {
    }

    /**
     * Authenticated Retrofit singleton (Bearer + refresh).
     */
    @NonNull
    public static Retrofit getClient(@NonNull Context context) {
        return authRetrofitClient.getClient(context.getApplicationContext());
    }
}
