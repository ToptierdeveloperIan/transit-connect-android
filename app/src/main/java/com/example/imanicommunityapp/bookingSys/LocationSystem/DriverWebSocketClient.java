// DriverWebSocketClient.java
package com.example.imanicommunityapp.bookingSys.LocationSystem;

import android.location.Location;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.google.gson.Gson;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DriverWebSocketClient {
    private static final String TAG = "DriverWS";
    private final OkHttpClient client = new OkHttpClient();
    private WebSocket webSocket;
    private final Gson gson = new Gson();
    private  ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final String wsUrl; // e.g. wss://yourserver/ws/driver-locationupdates/?driver_id=123
    private final TokenManager tokenManager;
    private final LocationProvider locationProvider; // small interface to fetch latest location

    public DriverWebSocketClient(String wsUrl, TokenManager tokenManager, LocationProvider locationProvider) {
        this.wsUrl = wsUrl;
        this.tokenManager = tokenManager;
        this.locationProvider = locationProvider;
    }

    public void connect() {
        Request req = new Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken()) // if you use token on ws
                .build();

        webSocket = client.newWebSocket(req, new WebSocketListener(){
            @Override public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WS open");
                startSending();
            }
            @Override public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "WS msg: " + text);
            }
            @Override public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                Log.e(TAG, "WS failure", t);
                stopSending();
                // optionally schedule reconnect with backoff
            }
            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WS closed: " + reason);
                stopSending();
            }
        });
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client closing");
            webSocket = null;
        }
        stopSending();
    }

    private void startSending() {
        if (running.compareAndSet(false, true)) {

            scheduler = Executors.newSingleThreadScheduledExecutor();

            scheduler.scheduleWithFixedDelay(() -> {
                try {
                    if (webSocket == null) return;

                    Location loc = locationProvider.getLastKnownLocation();
                    if (loc == null) return;

                    LocationPayload payload = new LocationPayload(
                            "location",
                            new LocData(loc.getLatitude(), loc.getLongitude())
                    );

                    webSocket.send(gson.toJson(payload));

                } catch (Exception e) {
                    Log.e(TAG, "send error", e);
                }
            }, 0, 3, TimeUnit.SECONDS);
        }
    }

    private void stopSending() {
        if (running.compareAndSet(true, false)) {
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler=null;
            }
        }
    }


    // payload structures
    private static class LocationPayload {
        String action;
        LocData data;
        LocationPayload(String action, LocData data) { this.action = action; this.data = data; }
    }
    private static class LocData {
        double lat, lng;
        LocData(double lat, double lng) { this.lat = lat; this.lng = lng; }
    }

    // simple provider interface to decouple location retrieval logic
    public interface LocationProvider {
        android.location.Location getLastKnownLocation();
    }
}
