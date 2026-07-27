package com.example.imanicommunityapp.auth.Repository;

import android.content.Context;

import androidx.annotation.NonNull;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Single entry point for all backend ({@code /api/}) Retrofit usage.
 *
 * <p><b>Plain client</b> — login, OTP, register, token refresh.
 * No Bearer interceptor and no authenticator (avoids refresh loops).
 *
 * <p><b>Authenticated client</b> — all post-login API traffic.
 * Attaches access tokens and refreshes on 401 via {@link TokenAuthenticator}.
 *
 * <p>Process startup: {@code ImaniApp.onCreate()} calls {@link #init(Context)} and
 * {@link #getClient(Context)} so the hub is ready before any Activity. Callers may
 * still use {@link #getClient(Context)} safely anytime. External hosts (e.g. Google
 * Maps) keep their own clients — see {@code NETWORK.md}.
 */
public final class authRetrofitClient {

    private static final String BASE_URL = "http://10.0.2.2:8000/api/";

    private static volatile Retrofit plainRetrofit;
    private static volatile Retrofit authenticatedRetrofit;
    private static volatile TokenManager tokenManager;
    private static volatile OkHttpClient plainOkHttp;
    private static volatile OkHttpClient authenticatedOkHttp;

    private authRetrofitClient() {
    }

    /**
     * Ensures {@link TokenManager} is bound to the application context.
     * Safe to call multiple times.
     */
    public static void init(@NonNull Context context) {
        if (tokenManager != null) {
            return;
        }
        synchronized (authRetrofitClient.class) {
            if (tokenManager == null) {
                tokenManager = new TokenManager(context.getApplicationContext());
            }
        }
    }

    /**
     * Unauthenticated backend client (login / OTP / register / refresh).
     * Thread-safe lazy singleton.
     */
    @NonNull
    public static Retrofit getPlainClient() {
        Retrofit local = plainRetrofit;
        if (local == null) {
            synchronized (authRetrofitClient.class) {
                local = plainRetrofit;
                if (local == null) {
                    plainOkHttp = new OkHttpClient.Builder().build();
                    local = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(plainOkHttp)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    plainRetrofit = local;
                }
            }
        }
        return local;
    }

    /**
     * Authenticated backend client (Bearer + 401 refresh). Preferred API entry.
     * Thread-safe lazy singleton; first call must supply a non-null context.
     */
    @NonNull
    public static Retrofit getClient(@NonNull Context context) {
        init(context);
        Retrofit local = authenticatedRetrofit;
        if (local == null) {
            synchronized (authRetrofitClient.class) {
                local = authenticatedRetrofit;
                if (local == null) {
                    authenticatedOkHttp = new OkHttpClient.Builder()
                            .addInterceptor(new authInterceptor(tokenManager))
                            .authenticator(new TokenAuthenticator(tokenManager))
                            .build();
                    local = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(authenticatedOkHttp)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    authenticatedRetrofit = local;
                }
            }
        }
        return local;
    }

    /**
     * Returns the authenticated client after {@link #getClient(Context)} / {@link #init(Context)}.
     *
     * @throws IllegalStateException if never initialized with a context
     */
    @NonNull
    public static Retrofit getClient() {
        Retrofit local = authenticatedRetrofit;
        if (local != null) {
            return local;
        }
        throw new IllegalStateException(
                "authRetrofitClient not initialized. Call getClient(Context) first.");
    }

    @NonNull
    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Shared {@link TokenManager} after init. Null only before first authenticated use.
     */
    public static TokenManager getTokenManager() {
        return tokenManager;
    }
}
