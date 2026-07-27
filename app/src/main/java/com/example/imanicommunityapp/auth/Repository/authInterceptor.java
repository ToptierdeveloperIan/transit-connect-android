package com.example.imanicommunityapp.auth.Repository;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches the current access token when present.
 * Token refresh is handled by {@link TokenAuthenticator}, not here.
 */
public class authInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public authInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String accessToken = tokenManager.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            request = request.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
        }

        return chain.proceed(request);
    }
}
