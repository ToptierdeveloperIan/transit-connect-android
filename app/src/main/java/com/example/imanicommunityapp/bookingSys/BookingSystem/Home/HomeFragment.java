package com.example.imanicommunityapp.bookingSys.BookingSystem.Home;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.Sync.InitialData;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingEvent;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingFlowState;
import com.example.imanicommunityapp.bookingSys.BookingSystem.ViewModel.BookingFlowViewModel;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingStatus;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap googleMaps;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private BookingFlowViewModel bookingFlowViewModel;
    private InitialData initialdata;

    private Context context;
    private Permissions_and_Resources mapResources;
    private HomeUI homeUI;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireContext();
        mapResources = new Permissions_and_Resources(context);
        homeUI = new HomeUI();
        homeUI.bind(view, context);

        bookingFlowViewModel = new ViewModelProvider(requireActivity()).get(BookingFlowViewModel.class);
        bookingFlowViewModel.initialize(requireContext());

        homeUI.setTriggerClickListener(v -> {
            BookingFlowState state = bookingFlowViewModel.getBookingState().getValue();
            BookingStatus status = state != null ? state.getStatus() : BookingStatus.IDLE;
            if (HomeUI.shouldCancelForStatus(status)) {
                cancelBooking();
            } else {
                openBookingFlow(view);
            }
        });

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(context.getApplicationContext(), getString(R.string.maps_api_key));
        }
        placesClient = Places.createClient(context);

        // Initialize location client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // UI logic
        homeUI.animatePlaceSearch(this::openBookingFlow);
        homeUI.triggerHamburgerIcon(requireActivity());
        observeBookingState();
        homeUI.showBottomSheet();
        homeUI.runEntranceAnimations(view);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMaps = map;
        mapResources.attachMap(map);

        LatLng defaultLocation = new LatLng(-1.286389, 36.817223);
        googleMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f));

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMaps.setMyLocationEnabled(true);
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMaps.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
                        }
                    });
        } else {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void cancelBooking() {
        bookingFlowViewModel.dispatch(BookingEvent.cancelRequested());
    }

    private void observeBookingState() {
        bookingFlowViewModel.getBookingState().observe(getViewLifecycleOwner(), this::renderBookingState);
    }

    private void renderBookingState(BookingFlowState state) {
        homeUI.renderBookingState(state, mapResources);
    }

    // Function responsible for Starting the booking flow
    private void openBookingFlow(View view) {
        BookingFlowState state = bookingFlowViewModel.getBookingState().getValue();
        if (state != null && state.getStatus() == BookingStatus.CANCELLING) return;
        bookingFlowViewModel.dispatch(BookingEvent.openBooking());
        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_homeFragment_to_selectStopFragment);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(googleMaps);
            } else {
                Toast.makeText(context, "Location permission is required.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
