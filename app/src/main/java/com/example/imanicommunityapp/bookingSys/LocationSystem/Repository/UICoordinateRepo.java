package com.example.imanicommunityapp.bookingSys.LocationSystem.Repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.auth.DataLayer.UserProfileRoomDb;
import com.example.imanicommunityapp.bookingSys.BookingResponse;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates repo: RAM fields + queries against booking_details (not user_profile).
 *
 * <p>Pricing: {@code fare} is base (always a number in RAM).
 * {@code discountedFare} is nullable — API may omit or send null.
 */
public class UICoordinateRepo {

    private static final String TAG = "UICoordinateRepo";

    private final UserProfileRoomDb userProfileRoomDb;
    private final Gson gson = new Gson();

    private boolean hasData = false;
    private int bookingId = -1;
    @Nullable
    private String quoteId;
    @Nullable
    private String message;
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    @Nullable
    private List<String> destinations;
    /** Base list fare. */
    private int fare;
    /**
     * Pay amount after promo. Null when not provided by API.
     */
    @Nullable
    private Double discountedFare;
    @Nullable
    private String errorMessage;

    public UICoordinateRepo(Context context) {
        this.userProfileRoomDb = UserProfileRoomDb.getInstance(context.getApplicationContext());
    }

    @Nullable
    public userCoordinates queryCoordinatesByUserId(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "queryCoordinatesByUserId: missing userId");
            return null;
        }

        UserProfileRoomDb.BookingDetailsEntity row =
                userProfileRoomDb.bookingDetailsDao().getLatestBookingForUser(userId);

        if (row == null) {
            Log.d(TAG, "No booking_details row for user_id=" + userId);
            return null;
        }

        List<String> destList = parseDestinations(row.destinationsJson);
        return new userCoordinates(
                row.startLat,
                row.startLng,
                row.endLat,
                row.endLng,
                destList,
                row.fare,
                row.discountedFare
        );
    }

    @Nullable
    public userCoordinates queryCoordinatesByUserIdIntoRam(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }

        UserProfileRoomDb.BookingDetailsEntity row =
                userProfileRoomDb.bookingDetailsDao().getLatestBookingForUser(userId);
        if (row == null) {
            return null;
        }

        List<String> destList = parseDestinations(row.destinationsJson);
        userCoordinates coords = new userCoordinates(
                row.startLat,
                row.startLng,
                row.endLat,
                row.endLng,
                destList,
                row.fare,
                row.discountedFare
        );

        storeCoordinatesInRam(coords);
        this.bookingId = row.bookingId;
        this.message = row.message;
        return coords;
    }

    /**
     * Store create-booking response into RAM.
     * Copies {@code discounted_fare} only when non-null on the model.
     */
    public void storeInRam(@Nullable BookingResponse response) {
        if (response == null) {
            clearRam();
            return;
        }

        this.bookingId = response.getBooking_id();
        this.message = response.getMessage();
        this.errorMessage = null;
        this.quoteId = null;
        applyCoordinatesToRam(response.getCoordinates());
        this.hasData = true;
    }

    /**
     * Store checkout (light path) response — no server booking_id.
     * bookingId stays -1; quoteId taken from body or coordinates.
     */
    public void storeCheckoutInRam(
            @Nullable String message,
            @Nullable String quoteId,
            @Nullable userCoordinates coords
    ) {
        this.bookingId = -1;
        this.message = message;
        this.errorMessage = null;
        this.quoteId = quoteId;
        if (this.quoteId == null && coords != null) {
            this.quoteId = coords.getQuoteId();
        }
        applyCoordinatesToRam(coords);
        this.hasData = true;
    }

    private void applyCoordinatesToRam(@Nullable userCoordinates coords) {
        if (coords != null) {
            this.startLat = coords.getStart_lat();
            this.startLng = coords.getStart_lng();
            this.endLat = coords.getEnd_lat();
            this.endLng = coords.getEnd_lng();
            this.fare = coords.getFare();
            this.discountedFare = coords.getDiscountedFare();
            if (coords.getDestinations() != null) {
                this.destinations = new ArrayList<>(coords.getDestinations());
            } else {
                this.destinations = null;
            }
            if (this.quoteId == null) {
                this.quoteId = coords.getQuoteId();
            }
        } else {
            this.startLat = 0d;
            this.startLng = 0d;
            this.endLat = 0d;
            this.endLng = 0d;
            this.destinations = null;
            this.fare = 0;
            this.discountedFare = null;
        }
    }

    public void storeCoordinatesInRam(@Nullable userCoordinates coords) {
        if (coords == null) {
            this.startLat = 0d;
            this.startLng = 0d;
            this.endLat = 0d;
            this.endLng = 0d;
            this.destinations = null;
            this.fare = 0;
            this.discountedFare = null;
            return;
        }
        this.startLat = coords.getStart_lat();
        this.startLng = coords.getStart_lng();
        this.endLat = coords.getEnd_lat();
        this.endLng = coords.getEnd_lng();
        this.destinations = coords.getDestinations() != null
                ? new ArrayList<>(coords.getDestinations())
                : null;
        this.fare = coords.getFare();
        this.discountedFare = coords.getDiscountedFare();
        this.hasData = true;
        this.errorMessage = null;
    }

    public void storeErrorInRam(@Nullable String error) {
        this.errorMessage = error;
    }

    public void clearRam() {
        this.hasData = false;
        this.bookingId = -1;
        this.quoteId = null;
        this.message = null;
        this.startLat = 0d;
        this.startLng = 0d;
        this.endLat = 0d;
        this.endLng = 0d;
        this.destinations = null;
        this.fare = 0;
        this.discountedFare = null;
        this.errorMessage = null;
    }

    public boolean hasData() {
        return hasData;
    }

    public int getBookingId() {
        return bookingId;
    }

    @Nullable
    public String getQuoteId() {
        return quoteId;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLng() {
        return startLng;
    }

    public double getEndLat() {
        return endLat;
    }

    public double getEndLng() {
        return endLng;
    }

    @Nullable
    public List<String> getDestinations() {
        return destinations;
    }

    public int getFare() {
        return fare;
    }

    /**
     * Nullable discounted/pay fare from last response. Null = not provided.
     */
    @Nullable
    public Double getDiscountedFare() {
        return discountedFare;
    }

    public boolean hasDiscountedFare() {
        return discountedFare != null;
    }

    /**
     * Display/charge helper: discounted if present, else base fare.
     */
    public double getDisplayOrPayAmount() {
        if (discountedFare != null) {
            return discountedFare;
        }
        return fare;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public userCoordinates getCoordinatesFromRam() {
        if (!hasData) {
            return null;
        }
        return new userCoordinates(
                startLat,
                startLng,
                endLat,
                endLng,
                destinations != null ? new ArrayList<>(destinations) : new ArrayList<>(),
                fare,
                discountedFare
        );
    }

    private List<String> parseDestinations(@Nullable String destinationsJson) {
        if (destinationsJson == null || destinationsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> list = gson.fromJson(destinationsJson, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse destinations_json", e);
            return new ArrayList<>();
        }
    }
}
