package com.example.imanicommunityapp.bookingSys;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.RetrofitClient;
import com.example.imanicommunityapp.auth.Repository.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RideStatusFragment extends Fragment {

    // UI
    private TextView statusBadge, driverName, vehicleDetails, pickupTime,
            eta, pickupLocation, dropoffLocation,
            paymentMethod, paymentStatus, driverRating;

    private EditText rideStatusText;

    // Logic
    private Context context;
    private BookingInterface bookingApi;
    private TokenManager tokenManager;
    private String bookingId;

    public RideStatusFragment() {
        super(R.layout.ridestatus);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---------------- CONTEXT ----------------
        context = requireContext();

        // ---------------- ARGUMENTS ----------------
        if (getArguments() != null) {
            bookingId = getArguments().getString("bookingId");
        }

        // ---------------- UI BINDING ----------------
        statusBadge = view.findViewById(R.id.statusBadge);
        rideStatusText = view.findViewById(R.id.rideStatusText);
        driverName = view.findViewById(R.id.driverName);
        vehicleDetails = view.findViewById(R.id.vehicleDetails);
        pickupTime = view.findViewById(R.id.pickupTime);
        eta = view.findViewById(R.id.eta);
        pickupLocation = view.findViewById(R.id.pickupLocation);
        dropoffLocation = view.findViewById(R.id.dropoffLocation);
        paymentMethod = view.findViewById(R.id.paymentMethod);
        paymentStatus = view.findViewById(R.id.paymentStatus);
        driverRating = view.findViewById(R.id.Driverrating);

        // Status field should be READ-ONLY
        rideStatusText.setEnabled(false);

        // ---------------- NETWORK ----------------
        tokenManager = new TokenManager(context);
        bookingApi = RetrofitClient
                .getClient(context)
                .create(BookingInterface.class);

        // ---------------- FETCH DATA ----------------
        fetchRideStatus();
    }

    // ---------------------------------------------------------------------
    // FETCH ACTIVE BOOKING FROM BACKEND
    // ---------------------------------------------------------------------
    private void fetchRideStatus() {

        Call<RideStatusResponse> call =
                bookingApi.getActiveBooking(tokenManager.getAccessToken());

        call.enqueue(new Callback<RideStatusResponse>() {
            @Override
            public void onResponse(Call<RideStatusResponse> call,
                                   Response<RideStatusResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    populateUI(response.body());
                } else {
                    Toast.makeText(context,
                            "No active ride found",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RideStatusResponse> call, Throwable t) {
                Toast.makeText(context,
                        "Failed to load ride status",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------------
    // POPULATE UI (SAFE + MATCHES XML)
    // ---------------------------------------------------------------------
    private void populateUI(RideStatusResponse booking) {

        // STATUS
        statusBadge.setText(booking.status);
        rideStatusText.setText(booking.status);
        applyStatusColor(booking.status);

        // DRIVER
        driverName.setText(
                booking.driver_name != null
                        ? booking.driver_name
                        : "Searching for driver..."
        );

        // VEHICLE
        vehicleDetails.setText(
                booking.bus_plate != null
                        ? booking.bus_plate
                        : "—"
        );

        // TIME
        pickupTime.setText(booking.eta);
        eta.setText("Arrives in " + booking.eta + " min");

        // LOCATIONS
        pickupLocation.setText(booking.pickup);
        dropoffLocation.setText(booking.destination);

        // PAYMENT
        paymentMethod.setText("M-Pesa"); // static for now
        paymentStatus.setText(booking.payment_status);

        // DRIVER RATING
        driverRating.setText(
                booking.status.equalsIgnoreCase("completed")
                        ? "★★★★★"
                        : "Pending"
        );
    }

    // ---------------------------------------------------------------------
    // STATUS COLOR LOGIC (NO UI CHANGE)
    // ---------------------------------------------------------------------
    private void applyStatusColor(String status) {

        int color;

        switch (status.toLowerCase()) {
            case "pending":
                color = getResources().getColor(R.color.yellow);
                break;

            case "confirmed":
                color = getResources().getColor(R.color.blue_primary);
                break;

            case "en route":
                color = getResources().getColor(R.color.green);
                break;

            case "completed":
                color = getResources().getColor(R.color.gray);
                break;

            case "cancelled":
                color = getResources().getColor(R.color.red);
                break;

            default:
                color = getResources().getColor(R.color.blue_light);
        }

        statusBadge.setBackgroundColor(color);
    }
}
