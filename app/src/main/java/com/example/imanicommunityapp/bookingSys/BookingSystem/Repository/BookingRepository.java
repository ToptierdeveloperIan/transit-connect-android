package com.example.imanicommunityapp.bookingSys.BookingSystem.Repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.imanicommunityapp.RetrofitClient;
import com.example.imanicommunityapp.auth.DataLayer.UserProfileRoomDb;
import com.example.imanicommunityapp.bookingSys.BookingInterface;
import com.example.imanicommunityapp.bookingSys.BookingResponse;
import com.example.imanicommunityapp.bookingSys.CancelRequest;
import com.example.imanicommunityapp.bookingSys.CancelResponse;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.BookingModel;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutRequest;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.CheckoutResponse;
import com.example.imanicommunityapp.bookingSys.LocationSystem.Repository.UICoordinateRepo;
import com.example.imanicommunityapp.bookingSys.BookingSystem.Models.userCoordinates;
import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BookingRepository {

    private static final String TAG = "BookingRepository";

    /**
     * Result of light checkout (no server Booking row).
     */
    public interface CheckoutCallback {
        void onSuccess(CheckoutResponse response);

        void onError(String message);
    }

    private final BookingInterface bookingAPI;
    private final UserProfileRoomDb userProfileRoomDb;
    private final UICoordinateRepo uiCoordinateRepo;
    private final ExecutorService databaseExecutor;
    private final Gson gson = new Gson();

    public BookingRepository(Context context) {
        Context appContext = context.getApplicationContext();
        Retrofit retrofit = RetrofitClient.getClient(appContext);
        bookingAPI = retrofit.create(BookingInterface.class);
        userProfileRoomDb = UserProfileRoomDb.getInstance(appContext);
        uiCoordinateRepo = new UICoordinateRepo(appContext);
        databaseExecutor = Executors.newSingleThreadExecutor();
    }

    public UICoordinateRepo getUiCoordinateRepo() {
        return uiCoordinateRepo;
    }

    /**
     * Light checkout: {@code POST bookings/checkout/}.
     * Validates route/stop, returns coords + fares + quote_id. No Booking.create.
     */
    public void checkout(CheckoutRequest request, @Nullable CheckoutCallback callback) {
        bookingAPI.checkout(request).enqueue(new Callback<CheckoutResponse>() {
            @Override
            public void onResponse(Call<CheckoutResponse> call, Response<CheckoutResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    CheckoutResponse body = response.body();
                    uiCoordinateRepo.storeCheckoutInRam(
                            body.getMessage(),
                            body.getQuoteId(),
                            body.getCoordinates()
                    );
                    if (callback != null) {
                        callback.onSuccess(body);
                    }
                    return;
                }

                String message = "Checkout failed.";
                if (response.body() != null) {
                    if (response.body().getMessage() != null) {
                        message = response.body().getMessage();
                    } else if (response.body().getError() != null) {
                        message = response.body().getError();
                    }
                } else if (!response.isSuccessful()) {
                    message = "Checkout failed (" + response.code() + ").";
                }
                uiCoordinateRepo.storeErrorInRam(message);
                if (callback != null) {
                    callback.onError(message);
                }
            }

            @Override
            public void onFailure(Call<CheckoutResponse> call, Throwable t) {
                String message = t.getMessage() != null ? t.getMessage() : "Checkout network error.";
                uiCoordinateRepo.storeErrorInRam(message);
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    /**
     * Legacy: creates a booking on the server. Prefer {@link #checkout}.
     */
    public void createBooking(BookingModel booking) {
        bookingAPI.createBooking(booking).enqueue(new Callback<BookingResponse>() {
            @Override
            public void onResponse(Call<BookingResponse> call, Response<BookingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    BookingResponse body = response.body();
                    String userId = booking != null ? booking.getUserId() : null;

                    uiCoordinateRepo.storeInRam(body);
                    storeBookingToDb(userId, body);
                    return;
                }

                String message = "Failed to create booking.";
                if (response.body() != null && response.body().getMessage() != null) {
                    message = response.body().getMessage();
                }
                uiCoordinateRepo.storeErrorInRam(message);
            }

            @Override
            public void onFailure(Call<BookingResponse> call, Throwable t) {
                uiCoordinateRepo.storeErrorInRam(
                        t.getMessage() != null ? t.getMessage() : "Booking failed."
                );
            }
        });
    }

    public void cancelBooking(int bookingId) {
        bookingAPI.cancelBooking(new CancelRequest(bookingId)).enqueue(new Callback<CancelResponse>() {
            @Override
            public void onResponse(Call<CancelResponse> call, Response<CancelResponse> response) {
                // result available via network only; no app callback layer
                if (!response.isSuccessful()) {
                    Log.w(TAG, "cancelBooking failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<CancelResponse> call, Throwable t) {
                Log.e(TAG, "cancelBooking failed", t);
            }
        });
    }

    /**
     * Stores create-booking response fields into UserProfileRoomDb
     * with {@code userId} as foreign key → user_profile.user_id.
     */
    public void storeBookingToDb(@Nullable String userId, @Nullable BookingResponse response) {
        if (userId == null || userId.isEmpty() || response == null) {
            Log.w(TAG, "storeBookingToDb skipped: missing userId or response");
            return;
        }

        databaseExecutor.execute(() -> {
            try {
                ensureUserProfileParent(userId);

                userCoordinates coords = response.getCoordinates();
                double startLat = coords != null ? coords.getStart_lat() : 0d;
                double startLng = coords != null ? coords.getStart_lng() : 0d;
                double endLat = coords != null ? coords.getEnd_lat() : 0d;
                double endLng = coords != null ? coords.getEnd_lng() : 0d;
                int fare = coords != null ? coords.getFare() : 0;
                // discounted_fare is nullable in JSON — persist null when absent
                Double discountedFare = coords != null ? coords.getDiscountedFare() : null;

                String destinationsJson = null;
                if (coords != null && coords.getDestinations() != null) {
                    destinationsJson = gson.toJson(coords.getDestinations());
                }

                UserProfileRoomDb.BookingDetailsEntity entity =
                        new UserProfileRoomDb.BookingDetailsEntity(
                                userId,
                                response.getBooking_id(),
                                response.getMessage(),
                                startLat,
                                startLng,
                                endLat,
                                endLng,
                                destinationsJson,
                                fare,
                                discountedFare,
                                null,
                                System.currentTimeMillis()
                        );

                long rowId = userProfileRoomDb.bookingDetailsDao().saveBookingForUser(entity);
                Log.d(TAG, "Stored booking_id=" + response.getBooking_id()
                        + " user_id=" + userId + " rowId=" + rowId);
            } catch (Exception e) {
                Log.e(TAG, "storeBookingToDb failed", e);
            }
        });
    }

    private void ensureUserProfileParent(String userId) {
        UserProfileRoomDb.UserProfileEntity profile =
                userProfileRoomDb.userProfileDao().getUserProfileByUserId(userId);
        if (profile != null) {
            return;
        }

        profile = userProfileRoomDb.userProfileDao().getUserProfile();
        if (profile == null) {
            userProfileRoomDb.userProfileDao().saveUserProfile(
                    new UserProfileRoomDb.UserProfileEntity(userId, null, null, null, null)
            );
            return;
        }

        if (profile.userId == null || profile.userId.isEmpty()) {
            profile.userId = userId;
            userProfileRoomDb.userProfileDao().saveUserProfile(profile);
        } else if (!userId.equals(profile.userId)) {
            throw new IllegalStateException("user_profile.user_id does not match " + userId);
        }
    }
}
