package com.example.imanicommunityapp.auth.Repository;

import android.util.Log;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.auth.DataLayer.LoginTokenService;
import com.example.imanicommunityapp.auth.Model.RefreshTokenRequest;
import com.example.imanicommunityapp.auth.Model.RefreshTokenResponse;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;

/**
 * Refreshes access tokens on 401 using the plain (unauthenticated) Retrofit client
 * so refresh never re-enters this authenticator.
 */
public class TokenAuthenticator implements Authenticator {

    private static final String TAG = "TokenAuthenticator";

    private final TokenManager tokenManager;
    private final LoginTokenService apiService;

    public TokenAuthenticator(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        // Plain client only — no Bearer / no Authenticator on the refresh call.
        this.apiService = authRetrofitClient.getPlainClient().create(LoginTokenService.class);
    }

    @Nullable
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.request().header("Authorization-Attempt") != null) {
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            Log.d(TAG, "Refresh token is null or empty");
            return null;
        }

        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        Call<RefreshTokenResponse> call = apiService.refreshToken(request);
        retrofit2.Response<RefreshTokenResponse> refreshResponse = call.execute();

        if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
            Log.d(TAG, "Token refresh failed");
            return null;
        }

        RefreshTokenResponse body = refreshResponse.body();
        String newAccess = body.getAccessToken();
        if (newAccess == null || newAccess.isEmpty()) {
            Log.d(TAG, "Refresh response missing access token");
            return null;
        }

        tokenManager.saveAccessToken(newAccess);
        String newRefresh = body.getRefreshToken();
        if (newRefresh != null && !newRefresh.isEmpty()) {
            tokenManager.saveRefreshToken(newRefresh);
        }

        Log.d(TAG, "Access token refreshed");
        return response.request().newBuilder()
                .header("Authorization", "Bearer " + newAccess)
                .header("Authorization-Attempt", "2")
                .build();
    }
}
