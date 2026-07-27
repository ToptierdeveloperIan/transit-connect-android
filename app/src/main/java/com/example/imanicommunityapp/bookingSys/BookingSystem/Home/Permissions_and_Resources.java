package com.example.imanicommunityapp.bookingSys.BookingSystem.Home;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.bookingSys.DirectionsAPIService;
import com.example.imanicommunityapp.bookingSys.GoogleMapsRetrofitClient;
import com.example.imanicommunityapp.bookingSys.directionsAPIResponse;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

/**
 * Map routing resources: fetches directions from Google, draws polylines,
 * and frames the camera on the route. Keeps GoogleMap interaction out of the Fragment UI layer.
 */
public class Permissions_and_Resources {

    private static final String TAG = "DirectionsAPI";
    private static final int ROUTE_PADDING_PX = 100;
    private static final float POLYLINE_WIDTH = 10f;
    private static final long POLYLINE_FADE_IN_MS = 100L;

    private final Context appContext;
    @Nullable
    private GoogleMap googleMap;
    @Nullable
    private Polyline currentPolyline;

    public Permissions_and_Resources(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * Bind the live map instance. Call from {@code onMapReady}.
     */
    public void attachMap(@NonNull GoogleMap map) {
        this.googleMap = map;
    }

    /**
     * Triggered after the user has exited location selection and booking is confirmed.
     * Fetches a route from Google Directions and draws it on the attached map.
     */
    public void fetchAndDrawPolyline(@NonNull LatLng start, @NonNull LatLng end) {
        String origin = start.latitude + "," + start.longitude;
        String destination = end.latitude + "," + end.longitude;
        String apiKey = appContext.getString(R.string.maps_api_key);

        DirectionsAPIService service = GoogleMapsRetrofitClient
                .getClient()
                .create(DirectionsAPIService.class);

        Call<directionsAPIResponse> call = service.getDirections(origin, destination, apiKey);
        call.enqueue(new retrofit2.Callback<directionsAPIResponse>() {

            @Override
            public void onResponse(Call<directionsAPIResponse> call,
                                   retrofit2.Response<directionsAPIResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (!response.body().getRoutes().isEmpty()) {
                        String encodedPolyline = response.body()
                                .getRoutes()
                                .get(0)
                                .getOverviewPolyline()
                                .getPoints();
                        drawRouteOnMap(encodedPolyline);
                    } else {
                        Toast.makeText(appContext, "No route found", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "URL: https://maps.googleapis.com/maps/api/directions/json?origin="
                                + origin + "&destination=" + destination + "&key=" + apiKey);
                    }
                } else {
                    Log.e(TAG, "Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<directionsAPIResponse> call, Throwable t) {
                Log.e(TAG, "Failed: " + t.getMessage());
            }
        });
    }

    /**
     * Decodes an encoded polyline and draws it on the map with a short fade-in,
     * then frames the camera on the full path.
     */
    public void drawRouteOnMap(@Nullable String encodedPolyline) {
        if (googleMap == null || encodedPolyline == null) return;

        List<LatLng> path = decodePolyline(encodedPolyline);
        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        currentPolyline = googleMap.addPolyline(new PolylineOptions()
                .addAll(path)
                .width(POLYLINE_WIDTH)
                .color(Color.TRANSPARENT)
                .geodesic(true)
        );

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentPolyline != null) {
                currentPolyline.setColor(Color.BLUE);
            }
        }, POLYLINE_FADE_IN_MS);

        focusMapOnRoute(path);
    }

    /**
     * Animates the camera to fit the given path with padding.
     */
    public void focusMapOnRoute(@Nullable List<LatLng> path) {
        if (path == null || path.isEmpty() || googleMap == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, ROUTE_PADDING_PX);
        googleMap.animateCamera(cu);
    }

    /**
     * Removes any drawn route polyline from the map.
     */
    public void clearRoute() {
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }

        return poly;
    }
}
