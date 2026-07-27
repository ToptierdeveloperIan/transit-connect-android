package com.example.imanicommunityapp.bookingSys.LocationSystem;

import static android.os.Looper.getMainLooper;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.imanicommunityapp.GenericResponse;
import com.example.imanicommunityapp.bookingSys.AvailabilityRequest;
import com.example.imanicommunityapp.bookingSys.CancelAvailabilityRequest;
import com.example.imanicommunityapp.bookingSys.DriverApi;
import com.example.imanicommunityapp.bookingSys.DriverState;
import com.example.imanicommunityapp.RetrofitClient;
import com.example.imanicommunityapp.auth.Repository.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Driver home — supply-side lifecycle for drivers.
 *
 * <h2>Product contrast (read this first)</h2>
 * <p>
 * In this system the <b>rider (user)</b> has the freer path: open booking, pick route/stop,
 * request, cancel after commit, pay, etc. The <b>driver</b> is the constrained role.
 * These rules protect capacity and assignments — they are intentional marketplace locks,
 * not accidental friction.
 * </p>
 *
 * <h2>Driver states ({@link DriverState})</h2>
 * <pre>
 *   OFFLINE  ──set availability──►  AVAILABLE  ──start trip──►  TRIP_IN_PROGRESS
 *      ▲                              │                              │
 *      │                              │ cancel availability          │ end trip
 *      └──────── leave supply ────────┘  (only if server allows)     ▼
 *                                                              AVAILABLE (or OFFLINE)
 * </pre>
 *
 * <h2>Business rules (intended contract)</h2>
 *
 * <b>R1 — Opt-in supply</b><br>
 * A driver is not matchable until they successfully call set-availability.
 * Offline means “not in the pool.” Only action: set availability.
 *
 * <p><b>R2 — Available means two choices</b><br>
 * When AVAILABLE the driver may:
 * <ul>
 *   <li>Cancel availability (leave the pool), or</li>
 *   <li>Start a trip (begin active work + location sharing).</li>
 * </ul>
 * They cannot do both at once; UI shows only those actions for this state.
 *
 * <p><b>R3 — Cancel availability is server-gated (hard lock)</b><br>
 * The client asks the backend to cancel availability. The server may:
 * <ul>
 *   <li>{@code success} — driver may leave the pool → should become {@link DriverState#OFFLINE}</li>
 *   <li>{@code denied} — bookings already assigned → stay available; show “Cannot cancel”</li>
 * </ul>
 * Rule in plain language: <b>once bookings are assigned to you, you cannot quietly go offline.</b>
 * This is the core driver constraint (assignment lock).
 *
 * <p><b>R4 — Trip is not the same as availability</b><br>
 * Starting a trip moves to {@link DriverState#TRIP_IN_PROGRESS}. During a trip the driver
 * should end the trip (not cancel availability from this screen). Location streaming is for
 * the trip phase, not for idle “available.”
 *
 * <p><b>R5 — Start trip requires location permission</b><br>
 * No fine-location permission → do not start trip / do not stream location.
 *
 * <p><b>R6 — End trip stops tracking and returns to supply-ready</b><br>
 * End trip: disconnect location channel, clear last location, return to AVAILABLE
 * (product may later choose OFFLINE instead — document if that changes).
 *
 * <p><b>R7 — Backend is source of truth for deny/success</b><br>
 * Local {@link #currentState} must follow API outcomes for set/cancel availability.
 * Do not invent “cancelled” UI while still AVAILABLE, and do not assume AVAILABLE on open
 * without a successful set-availability (or a rehydrate API).
 *
 * <h2>Allowed actions by state (UI matrix)</h2>
 * <pre>
 *   OFFLINE           → [ Set availability ]
 *   AVAILABLE         → [ Cancel availability ]  [ Start trip ]
 *   TRIP_IN_PROGRESS  → [ End trip ]
 * </pre>
 *
 * <h2>API map</h2>
 * <ul>
 *   <li>Set availability  → {@link DriverApi#setAvailability}</li>
 *   <li>Cancel availability → {@link DriverApi#cancelAvailability} (may return denied)</li>
 *   <li>Start / end trip    → {@link DriverApi#startTrip} / {@link DriverApi#EndTrip}
 *       (intended; wire when implementing fully)</li>
 *   <li>Live location       → WebSocket {@code ws/driver/{id}/location/}</li>
 * </ul>
 *
 * <h2>Implementation notes (known gaps — do not treat as product rules)</h2>
 * <ul>
 *   <li>{@code currentState} currently defaults to AVAILABLE; product rule R1 implies OFFLINE.</li>
 *   <li>On cancel success, code may still set AVAILABLE; product rule R3 success → OFFLINE.</li>
 *   <li>END TRIP label may still invoke start-trip path until endTrip is wired on click.</li>
 *   <li>Cancel/start buttons must be attached to the view hierarchy to be usable.</li>
 * </ul>
 *
 * @see DriverState
 * @see DriverApi
 */
public class driverHomeFragment extends Fragment {

    private Button btnSetAvailability;
    private Button btnCancelAvailability;
    private Button btnStartTrip;
    private TextView hintStatus;

    private DriverWebSocketClient driverWebSocketClient;
    private FusedLocationProviderClient locationClient;
    private TokenManager tokenManager;
    private DriverApi driverApi;
    private Location latestLocation;

    /**
     * Local mirror of driver lifecycle. Prefer OFFLINE until set-availability succeeds (R1).
     * Default may not match backend until rehydrate/API is added.
     */
    private DriverState currentState = DriverState.AVAILABLE;
    private Handler locationHandler = new Handler(getMainLooper());
    private Runnable sendLocationRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        hintStatus = new TextView(requireContext());
        layout.addView(hintStatus);

        btnSetAvailability = new Button(requireContext());
        btnSetAvailability.setText("SET AVAILABILITY");
        layout.addView(btnSetAvailability);

        btnCancelAvailability = new Button(requireContext());
        btnCancelAvailability.setText("CANCEL AVAILABILITY");
        btnStartTrip = new Button(requireContext());
        btnStartTrip.setText("START TRIP");

        tokenManager = new TokenManager(requireContext());
        driverApi = RetrofitClient.getClient(requireContext()).create(DriverApi.class);

        locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        setupButtons();

        updateUIForState();

        return layout;
    }

    private void setupButtons() {
        // R1: set availability
        btnSetAvailability.setOnClickListener(v -> setAvailability());

        // R2 + R3: leave pool (server may deny if bookings assigned)
        btnCancelAvailability.setOnClickListener(v -> cancelAvailability());

        // R4 + R5: enter trip / (intended) end trip when label is END TRIP
        btnStartTrip.setOnClickListener(v -> startTrip());
    }

    /**
     * Renders the action matrix for {@link #currentState}.
     * OFFLINE → set only; AVAILABLE → cancel or start; TRIP → end only.
     */
    private void updateUIForState() {

        btnSetAvailability.setVisibility(View.GONE);
        btnCancelAvailability.setVisibility(View.GONE);
        btnStartTrip.setVisibility(View.GONE);

        switch (currentState) {

            case OFFLINE:
                // R1: not in pool — only opt-in
                hintStatus.setText("You are offline.");
                btnSetAvailability.setText("SET AVAILABILITY");
                btnSetAvailability.setVisibility(View.VISIBLE);
                break;

            case AVAILABLE:
                // R2: in pool — may leave (R3) or start trip (R4)
                hintStatus.setText("You are available.");
                btnCancelAvailability.setVisibility(View.VISIBLE);
                btnStartTrip.setText("START TRIP");
                btnStartTrip.setVisibility(View.VISIBLE);
                break;

            case TRIP_IN_PROGRESS:
                // R4: no cancel-availability here — end trip only
                hintStatus.setText("Trip in progress.");
                btnStartTrip.setText("END TRIP");
                btnStartTrip.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * R1 — Opt into the matching pool.
     * On success: {@link DriverState#AVAILABLE}.
     */
    private void setAvailability() {
        AvailabilityRequest req = new AvailabilityRequest(true, tokenManager.getUserID());

        driverApi.setAvailability(req).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    currentState = DriverState.AVAILABLE;
                    updateUIForState();
                    Toast.makeText(getContext(), "Availability set", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Could not set availability", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * R3 — Leave the pool only if the server allows.
     * <ul>
     *   <li>denied  → stay AVAILABLE (assignments lock the driver)</li>
     *   <li>success → should become OFFLINE (leave supply); verify local state matches</li>
     * </ul>
     */
    private void cancelAvailability() {
        CancelAvailabilityRequest req = new CancelAvailabilityRequest(tokenManager.getUserID());

        driverApi.cancelAvailability(req).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GenericResponse resp = response.body();

                    // R3 hard lock: assigned bookings block leaving the pool
                    if ("denied".equals(resp.getStatus())) {
                        Toast.makeText(getContext(),
                                "Bookings already assigned. Cannot cancel!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if ("success".equals(resp.getStatus())) {
                        // Intended: OFFLINE. Current code may still set AVAILABLE — fix when aligning R3.
                        currentState = DriverState.AVAILABLE;
                        updateUIForState();
                        Toast.makeText(getContext(), "Availability cancelled", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Backend error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * R4 + R5 — Begin trip: require location, start updates + WS, mark TRIP_IN_PROGRESS.
     * Prefer calling {@link DriverApi#startTrip} before flipping local state when wired.
     */
    private void startTrip() {
        // R5: no trip without location permission
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: prompt user for permission, then retry
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        startLocationUpdates();

        // Live location channel for this driver during the trip
        driverWebSocketClient = new DriverWebSocketClient(
                "ws/driver/" + tokenManager.getUserID() + "/location/",
                tokenManager,
                () -> {
                    if (latestLocation != null) {
                        return latestLocation;
                    }
                    return null;
                });

        driverWebSocketClient.connect();

        // Periodic reconnect/send loop (implementation detail; prefer single send loop on WS)
        sendLocationRunnable = new Runnable() {
            @Override
            public void run() {
                if (driverWebSocketClient != null) {
                    driverWebSocketClient.connect();

                    locationHandler.postDelayed(this, 3000);
                }
            }
        };
        locationHandler.post(sendLocationRunnable);

        currentState = DriverState.TRIP_IN_PROGRESS;
        updateUIForState();
    }

    /**
     * R6 — End trip: stop WS/location, clear last fix, return to AVAILABLE.
     * Prefer calling {@link DriverApi#EndTrip} when wired. Wire this to the END TRIP click.
     */
    private void endTrip() {
        if (driverWebSocketClient != null) {
            driverWebSocketClient.disconnect();
            driverWebSocketClient = null;
        }

        locationHandler.removeCallbacks(sendLocationRunnable);
        latestLocation = null;

        currentState = DriverState.AVAILABLE;
        updateUIForState();

        Toast.makeText(getContext(), "Trip ended", Toast.LENGTH_SHORT).show();
    }

    /** R5 helper — high-accuracy location updates for trip streaming. */
    private void startLocationUpdates() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(getContext(),
                    "Location permission not granted",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            com.google.android.gms.location.LocationRequest locationRequest =
                    com.google.android.gms.location.LocationRequest.create();

            locationRequest.setInterval(3000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(
                    com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            );

            locationClient.requestLocationUpdates(
                    locationRequest,
                    new com.google.android.gms.location.LocationCallback() {
                        @Override
                        public void onLocationResult(
                                com.google.android.gms.location.LocationResult result) {

                            if (result != null) {
                                latestLocation = result.getLastLocation();
                            }
                        }
                    },
                    getMainLooper()
            );

        } catch (SecurityException e) {
            Toast.makeText(getContext(),
                    "Location permission revoked",
                    Toast.LENGTH_SHORT).show();
        }
    }

}
