package com.example.imanicommunityapp.auth.DataLayer;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.imanicommunityapp.auth.Repository.authRetrofitClient;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Compatibility facade. Prefer {@link authRetrofitClient} directly.
 * All backend HTTP goes through the auth Retrofit singleton hub.
 */
public final class OkHttpclient {

    private OkHttpclient() {
    }

    /**
     * @deprecated Use {@link authRetrofitClient#getClient(Context)} and its OkHttp layer.
     * This method only ensures the authenticated singleton exists and cannot return
     * a standalone OkHttp without building Retrofit first.
     */
    @Deprecated
    @NonNull
    public static OkHttpClient getOkHttpClient() {
        // Authenticated Retrofit owns the OkHttp client; callers should use Retrofit APIs.
        // Returning a new plain client would silently drop auth — force hub usage instead.
        throw new UnsupportedOperationException(
                "Use authRetrofitClient.getClient(Context) / getPlainClient() instead of raw OkHttp.");
    }

    @NonNull
    public static Retrofit getAuthenticatedRetrofit(@NonNull Context context) {
        return authRetrofitClient.getClient(context);
    }

    @NonNull
    public static Retrofit getPlainRetrofit() {
        return authRetrofitClient.getPlainClient();
    }
}
