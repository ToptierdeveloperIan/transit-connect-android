package com.example.imanicommunityapp.bookingSys.BookingSystem.Models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Route geometry + destinations + pricing from backend
 * {@code get_route_coordinates} / create-booking {@code coordinates} JSON.
 *
 * <h2>JSON fields (API contract)</h2>
 * <ul>
 *   <li>{@code start_lat}, {@code start_lng}, {@code end_lat}, {@code end_lng}</li>
 *   <li>{@code destinations} — list of stop names</li>
 *   <li>{@code fare} / {@code base_fare} — list price (base). Treated as 0 if omitted.</li>
 *   <li>{@code discounted_fare} — <b>nullable</b>. Pay amount after promo.
 *       May be {@code null} when pricing was not applied, quote incomplete,
 *       or older servers omit the field. UI/payment must null-check.</li>
 * </ul>
 *
 * <p>Payment amount rule (client):
 * {@code discounted_fare != null ? discounted_fare : fare}
 * (fallback to base only when discounted is absent — not a substitute for a server quote).
 *
 * <p>See also: {@code docs} / backend {@code FARE_QUOTE.md}.
 */
public class userCoordinates implements Parcelable {

    private double start_lat;
    private double start_lng;
    private double end_lat;
    private double end_lng;
    private List<String> destinations;

    /**
     * Base / list fare ({@code fare} or {@code base_fare} on API).
     * Primitive: missing JSON → 0.
     */
    private int fare;

    /**
     * Optional alias for base list price if API sends {@code base_fare}.
     * Prefer {@link #getBaseFare()} which resolves fare/base_fare.
     */
    @SerializedName("base_fare")
    @Nullable
    private Double base_fare;

    /**
     * Pay amount after discount. <b>JSON may be null</b> — never unbox without a check.
     */
    @SerializedName("discounted_fare")
    @Nullable
    private Double discounted_fare;

    /**
     * Optional FareQuote id from backend pricing (when user-authenticated quote was stored).
     */
    @SerializedName("quote_id")
    @Nullable
    private String quote_id;

    public userCoordinates() {
        destinations = new ArrayList<>();
        fare = 0;
        base_fare = null;
        discounted_fare = null;
    }

    public userCoordinates(
            double start_lat,
            double start_lng,
            double end_lat,
            double end_lng,
            List<String> destinations
    ) {
        this(start_lat, start_lng, end_lat, end_lng, destinations, 0, null);
    }

    public userCoordinates(
            double start_lat,
            double start_lng,
            double end_lat,
            double end_lng,
            List<String> destinations,
            int fare
    ) {
        this(start_lat, start_lng, end_lat, end_lng, destinations, fare, null);
    }

    public userCoordinates(
            double start_lat,
            double start_lng,
            double end_lat,
            double end_lng,
            List<String> destinations,
            int fare,
            @Nullable Double discounted_fare
    ) {
        this.start_lat = start_lat;
        this.start_lng = start_lng;
        this.end_lat = end_lat;
        this.end_lng = end_lng;
        this.destinations = destinations;
        this.fare = fare;
        this.base_fare = (double) fare;
        this.discounted_fare = discounted_fare;
    }

    protected userCoordinates(Parcel in) {
        start_lat = in.readDouble();
        start_lng = in.readDouble();
        end_lat = in.readDouble();
        end_lng = in.readDouble();
        destinations = in.createStringArrayList();
        fare = in.readInt();
        if (in.readByte() == 0) {
            base_fare = null;
        } else {
            base_fare = in.readDouble();
        }
        if (in.readByte() == 0) {
            discounted_fare = null;
        } else {
            discounted_fare = in.readDouble();
        }
    }

    public static final Creator<userCoordinates> CREATOR = new Creator<userCoordinates>() {
        @Override
        public userCoordinates createFromParcel(Parcel in) {
            return new userCoordinates(in);
        }

        @Override
        public userCoordinates[] newArray(int size) {
            return new userCoordinates[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(start_lat);
        dest.writeDouble(start_lng);
        dest.writeDouble(end_lat);
        dest.writeDouble(end_lng);
        dest.writeStringList(destinations);
        dest.writeInt(fare);
        if (base_fare == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(base_fare);
        }
        if (discounted_fare == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(discounted_fare);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public double getStart_lat() {
        return start_lat;
    }

    public double getStart_lng() {
        return start_lng;
    }

    public double getEnd_lat() {
        return end_lat;
    }

    public double getEnd_lng() {
        return end_lng;
    }

    public List<String> getDestinations() {
        return destinations;
    }

    /** Raw {@code fare} field (base list price; 0 if omitted). */
    public int getFare() {
        return fare;
    }

    /**
     * Base list price: prefers {@code base_fare} JSON when present, else {@code fare}.
     */
    public double getBaseFare() {
        if (base_fare != null) {
            return base_fare;
        }
        return fare;
    }

    /**
     * Discounted / pay fare from JSON. <b>May be null</b> — do not unbox blindly.
     */
    @Nullable
    public Double getDiscounted_fare() {
        return discounted_fare;
    }

    /**
     * Same as {@link #getDiscounted_fare()} with clearer Java naming.
     */
    @Nullable
    public Double getDiscountedFare() {
        return discounted_fare;
    }

    /**
     * Whether the API provided a discounted (pay) amount.
     */
    public boolean hasDiscountedFare() {
        return discounted_fare != null;
    }

    /**
     * Amount to show/charge on the client when a full server quote is not used:
     * discounted if non-null, otherwise base fare.
     *
     * <p>Prefer server {@code quote_id} + payment bridge when available.
     */
    public double getDisplayOrPayAmount() {
        if (discounted_fare != null) {
            return discounted_fare;
        }
        return getBaseFare();
    }

    public void setStart_lat(double start_lat) {
        this.start_lat = start_lat;
    }

    public void setStart_lng(double start_lng) {
        this.start_lng = start_lng;
    }

    public void setEnd_lat(double end_lat) {
        this.end_lat = end_lat;
    }

    public void setEnd_lng(double end_lng) {
        this.end_lng = end_lng;
    }

    public void setDestinations(List<String> destinations) {
        this.destinations = destinations;
    }

    public void setFare(int fare) {
        this.fare = fare;
    }

    public void setBase_fare(@Nullable Double base_fare) {
        this.base_fare = base_fare;
    }

    public void setDiscounted_fare(@Nullable Double discounted_fare) {
        this.discounted_fare = discounted_fare;
    }

    public void setDiscountedFare(@Nullable Double discounted_fare) {
        this.discounted_fare = discounted_fare;
    }

    @Nullable
    public String getQuoteId() {
        return quote_id;
    }

    public void setQuoteId(@Nullable String quote_id) {
        this.quote_id = quote_id;
    }
}
