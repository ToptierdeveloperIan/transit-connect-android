package com.example.imanicommunityapp.bookingSys.BookingSystem.Home;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.imanicommunityapp.R;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingFlowState;
import com.example.imanicommunityapp.bookingSys.BookingSystem.BookingStatus.BookingStatus;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;
import com.google.android.gms.maps.model.LatLng;

/**
 * Home screen view binder: owns view references, click wiring, status rendering,
 * overlays, and entrance animations. Keeps presentation out of {@link HomeFragment}.
 */
public class HomeUI {

    private Context context;
    private LinearLayout bottomsheet;
    private TextView whereto;
    private TextView bookingStatusText;
    private View overlay;
    private View bottompanel;
    private View toppanel;
    private ImageView hamburgerIcon;
    private Button triggerButton;
    private Animation slidebottom;
    private Animation slidetop;

    private View driverSearchOverlay;
    private TextView driverSearchText;

    /**
     * Binds layout views, loads animations, and installs the driver-search overlay.
     */
    public void bind(@NonNull View root, @NonNull Context context) {
        this.context = context;

        bottomsheet = root.findViewById(R.id.bottomsheet);
        overlay = root.findViewById(R.id.animationoverlay);
        toppanel = root.findViewById(R.id.topPanel);
        bottompanel = root.findViewById(R.id.bottomPanel);
        whereto = root.findViewById(R.id.whereto);
        bookingStatusText = root.findViewById(R.id.bookingStatusText);
        hamburgerIcon = root.findViewById(R.id.hamburgerIcon);
        triggerButton = root.findViewById(R.id.trigger);

        slidetop = AnimationUtils.loadAnimation(context, R.anim.slidefromtop);
        slidebottom = AnimationUtils.loadAnimation(context, R.anim.slidefrombottom);

        setupDriverSearchOverlay(root);
    }

    private void setupDriverSearchOverlay(@NonNull View root) {
        driverSearchOverlay = new View(context);
        driverSearchOverlay.setBackgroundColor(Color.parseColor("#80000000"));
        driverSearchOverlay.setVisibility(View.GONE);

        driverSearchText = new TextView(context);
        driverSearchText.setText("Searching for available drivers...");
        driverSearchText.setTextColor(Color.WHITE);
        driverSearchText.setTextSize(16f);
        driverSearchText.setPadding(24, 24, 24, 24);
        driverSearchText.setVisibility(View.GONE);

        ((ViewGroup) root).addView(driverSearchOverlay);
        ((ViewGroup) root).addView(driverSearchText);
    }

    public void setTriggerClickListener(@Nullable View.OnClickListener listener) {
        if (triggerButton != null) {
            triggerButton.setOnClickListener(listener);
        }
    }

    public void triggerHamburgerIcon(@NonNull Activity activity) {
        if (hamburgerIcon == null) return;
        hamburgerIcon.setOnClickListener(v -> {
            DrawerLayout drawerLayout = activity.findViewById(R.id.drawerLayout);
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    public void animatePlaceSearch(@Nullable View.OnClickListener listener) {
        if (whereto != null) {
            whereto.setOnClickListener(listener);
        }
    }

    public void showBottomSheet() {
        if (bottomsheet != null) {
            bottomsheet.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Entrance polish: slide bottom sheet, fade in trigger, show driver-search overlay.
     */
    public void runEntranceAnimations(@NonNull View root) {
        root.post(() -> {
            if (bottomsheet != null) {
                bottomsheet.setTranslationY(bottomsheet.getHeight());
                bottomsheet.animate()
                        .translationY(0)
                        .setDuration(400)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }

            if (triggerButton != null) {
                triggerButton.setAlpha(0f);
                triggerButton.animate().alpha(1f).setDuration(600).start();
            }

            showDriverSearchOverlay();
        });
    }

    public void updateToCancelState() {
        if (triggerButton != null) {
            triggerButton.setText("Cancel Ride");
        }
    }

    /**
     * Renders booking status on the home chrome. Route draw/clear is delegated to
     * {@link Permissions_and_Resources} with the same call order as before.
     */
    public void renderBookingState(@Nullable BookingFlowState state,
                                   @NonNull Permissions_and_Resources mapResources) {
        if (state == null) return;

        switch (state.getStatus()) {
            case IDLE:
            case SELECTING_ROUTE:
            case SELECTING_DROPOFF:
                triggerButton.setText("Request Ride");
                bookingStatusText.setVisibility(View.GONE);
                break;
            case SUBMITTING_BOOKING:
                bookingStatusText.setText("Submitting booking...");
                bookingStatusText.setTextColor(Color.parseColor("#1B5E20"));
                bookingStatusText.setVisibility(View.VISIBLE);
                break;
            case BOOKING_CONFIRMED:
                updateToCancelState();
                bookingStatusText.setText("Booked: " + state.getRouteName() + " -> " + state.getDropOffName());
                bookingStatusText.setTextColor(Color.parseColor("#1B5E20"));
                bookingStatusText.setVisibility(View.VISIBLE);

                userCoordinates coords = state.getCoordinates();
                if (coords != null) {
                    LatLng start = new LatLng(coords.getStart_lat(), coords.getStart_lng());
                    LatLng end = new LatLng(coords.getEnd_lat(), coords.getEnd_lng());
                    mapResources.fetchAndDrawPolyline(start, end);
                }
                break;
            case DRIVER_MATCHING:
                updateToCancelState();
                bookingStatusText.setText("Finding driver...");
                bookingStatusText.setTextColor(Color.parseColor("#1B5E20"));
                bookingStatusText.setVisibility(View.VISIBLE);
                break;
            case RIDE_ACTIVE:
                updateToCancelState();
                bookingStatusText.setText("Ride in progress");
                bookingStatusText.setTextColor(Color.parseColor("#1B5E20"));
                bookingStatusText.setVisibility(View.VISIBLE);
                break;
            case CANCELLING:
                bookingStatusText.setText("Cancelling booking...");
                bookingStatusText.setTextColor(Color.parseColor("#B45309"));
                bookingStatusText.setVisibility(View.VISIBLE);
                triggerButton.setEnabled(false);
                break;
            case CANCELLED:
                mapResources.clearRoute();
                triggerButton.setEnabled(true);
                triggerButton.setText("Request Ride");
                bookingStatusText.setText("Booking cancelled");
                bookingStatusText.setTextColor(Color.parseColor("#4B5563"));
                bookingStatusText.setVisibility(View.VISIBLE);
                break;
            case ERROR:
                triggerButton.setEnabled(true);
                triggerButton.setText("Request Ride");
                bookingStatusText.setText(state.getErrorMessage() != null
                        ? state.getErrorMessage()
                        : "Something went wrong.");
                bookingStatusText.setTextColor(Color.RED);
                bookingStatusText.setVisibility(View.VISIBLE);
                break;
        }
    }

    public void showDriverSearchOverlay() {
        if (driverSearchOverlay == null || driverSearchText == null) return;

        driverSearchOverlay.setVisibility(View.VISIBLE);
        driverSearchText.setVisibility(View.VISIBLE);

        driverSearchOverlay.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> new Handler(Looper.getMainLooper())
                        .postDelayed(this::hideDriverSearchOverlay, 2000))
                .start();

        driverSearchText.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
    }

    public void hideDriverSearchOverlay() {
        if (driverSearchOverlay == null || driverSearchText == null) return;

        driverSearchOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> driverSearchOverlay.setVisibility(View.GONE))
                .start();

        driverSearchText.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> driverSearchText.setVisibility(View.GONE))
                .start();
    }

    /**
     * Resolves whether the primary CTA should cancel or open booking, matching prior logic.
     */
    public static boolean shouldCancelForStatus(@Nullable BookingStatus status) {
        return status == BookingStatus.BOOKING_CONFIRMED
                || status == BookingStatus.DRIVER_MATCHING
                || status == BookingStatus.RIDE_ACTIVE;
    }
}
