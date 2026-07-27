package com.example.imanicommunityapp.auth.DataLayer;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;

/**
 * Local app database.
 *
 * user_profile.user_id  (unique)
 *          ^
 *          | FK CASCADE
 * booking_details.user_id
 */
@Database(
        entities = {
                UserProfileRoomDb.UserProfileEntity.class,
                UserProfileRoomDb.SyncMetadataEntity.class,
                UserProfileRoomDb.BookingDetailsEntity.class,
                UserProfileRoomDB.RouteDestination.class
        },
        version = 7,
        exportSchema = false
)
public abstract class UserProfileRoomDb extends RoomDatabase {

    private static final String DATABASE_NAME = "user_profile.db";
    private static volatile UserProfileRoomDb INSTANCE;

    public abstract UserProfileDao userProfileDao();

    public abstract SyncMetadataDao syncMetadataDao();

    public abstract BookingDetailsDao bookingDetailsDao();


    // Get Instance of the Database method.
    public static UserProfileRoomDb getInstance(Context context) {
        if (INSTANCE == null) {
            //thread safety protection since we are using the singleton pattern
            synchronized (UserProfileRoomDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    UserProfileRoomDb.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Entity(
            tableName = "user_profile",
            indices = {@Index(value = "user_id", unique = true)}
    )
    public static class UserProfileEntity {

        /** Single-row profile table (matches existing queries using id = 1). */
        @PrimaryKey
        public int id = 1;

        @ColumnInfo(name = "user_id")
        public String userId;

        @ColumnInfo(name = "user_role")
        public String userRole;

        @ColumnInfo(name = "first_name")
        public String firstName;

        @ColumnInfo(name = "second_name")
        public String secondName;

        @ColumnInfo(name = "phone_no")
        public String phoneNo;

        @ColumnInfo(name = "status")
        public boolean status;

        /**
         * Last known server ResourceVersion for profile (datasync + UserSettings).
         * Used as base_version on name updates for optimistic concurrency.
         */
        @ColumnInfo(name = "profile_version")
        public int profileVersion;

        /**
         * True when names were changed offline and are waiting for queue drain.
         * Rehydrate must not blindly clobber these without version rules.
         */
        @ColumnInfo(name = "pending_name_mutation")
        public boolean pendingNameMutation;

        @ColumnInfo(name = "pending_mutation_id")
        @Nullable
        public String pendingMutationId;

        @ColumnInfo(name = "pending_base_version")
        public int pendingBaseVersion;

        /**
         * Draft only: number awaiting OTP. Never treat as account phone for login.
         */
        @ColumnInfo(name = "phone_pending_verification")
        @Nullable
        public String phonePendingVerification;

        public UserProfileEntity(String userId, String userRole, String firstName, String secondName, String phoneNo) {
            this.id = 1;
            this.userId = userId;
            this.userRole = userRole;
            this.firstName = firstName;
            this.secondName = secondName;
            this.phoneNo = phoneNo;
            this.status = false;
            this.profileVersion = 0;
            this.pendingNameMutation = false;
            this.pendingMutationId = null;
            this.pendingBaseVersion = 0;
            this.phonePendingVerification = null;
        }
    }

    /**
     * Database model representing cached route destinations and their GPS coordinates.
     * Used to store static route coordinates offline for clean autocomplete and maps loading.
     */
    @Entity(
            tableName = "route_destinations",
            indices = {@Index(value = {"route", "destination"}, unique = true)}
    )
    public static class RouteDestination {

        @PrimaryKey(autoGenerate = true)
        public int id;

        @NonNull
        @ColumnInfo(name = "route")
        public String route;

        @NonNull
        @ColumnInfo(name = "destination")
        public String destination;


        @ColumnInfo(name = "lat")
        public double lat;

        @ColumnInfo(name = "lng")
        public double lng;

        public RouteDestination(@NonNull String route, @NonNull String destination, double lat, double lng) {
            this.route = route;
            this.destination = destination;
            this.lat = lat;
            this.lng = lng;
        }

        @NonNull
        public String getRoute() {
            return route;
        }

        @NonNull
        public String getDestination() {
            return destination;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        @NonNull
        @Override
        public String toString() {
            return "RouteDestination{" +
                    "id=" + id +
                    ", route='" + route + '\'' +
                    ", destination='" + destination + '\'' +
                    ", lat=" + lat +
                    ", lng=" + lng +
                    '}';
        }
    }

    @Entity(tableName = "sync_metadata")
    public static class SyncMetadataEntity {

        @PrimaryKey
        public int id = 1;

        @ColumnInfo(name = "data_type")
        public String dataType;

        @ColumnInfo(name = "local_data_present")
        public boolean localDataPresent;

        @ColumnInfo(name = "fetch_attempted")
        public boolean fetchAttempted;

        @ColumnInfo(name = "sync_in_progress")
        public boolean syncInProgress;

        @ColumnInfo(name = "queued_for_retry")
        public boolean queuedForRetry;

        @ColumnInfo(name = "last_successful_sync_at")
        public long lastSuccessfulSyncAt;

        @ColumnInfo(name = "last_sync_message")
        public String lastSyncMessage;

        public SyncMetadataEntity(
                String dataType,
                boolean localDataPresent,
                boolean fetchAttempted,
                boolean syncInProgress,
                boolean queuedForRetry,
                long lastSuccessfulSyncAt,
                String lastSyncMessage
        ) {
            this.dataType = dataType;
            this.localDataPresent = localDataPresent;
            this.fetchAttempted = fetchAttempted;
            this.syncInProgress = syncInProgress;
            this.queuedForRetry = queuedForRetry;
            this.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
            this.lastSyncMessage = lastSyncMessage;
        }
    }

    /**
     * Create-booking response body, linked to the logged-in user via user_id FK.
     */
    @Entity(
            tableName = "booking_details",
            foreignKeys = @ForeignKey(
                    entity = UserProfileEntity.class,
                    parentColumns = "user_id",
                    childColumns = "user_id",
                    onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.CASCADE
            ),
            indices = {
                    @Index("user_id"),
                    @Index(value = {"user_id", "booking_id"}, unique = true)
            }
    )
    public static class BookingDetailsEntity {

        @PrimaryKey(autoGenerate = true)
        public long id;

        @NonNull
        @ColumnInfo(name = "user_id")
        public String userId;

        @ColumnInfo(name = "booking_id")
        public int bookingId;

        @Nullable
        @ColumnInfo(name = "message")
        public String message;

        @ColumnInfo(name = "start_lat")
        public double startLat;

        @ColumnInfo(name = "start_lng")
        public double startLng;

        @ColumnInfo(name = "end_lat")
        public double endLat;

        @ColumnInfo(name = "end_lng")
        public double endLng;

        @Nullable
        @ColumnInfo(name = "destinations_json")
        public String destinationsJson;

        /** Base list fare from API coordinates.fare / base_fare. */
        @ColumnInfo(name = "fare")
        public int fare;

        /**
         * Pay amount after promo. Null when API omitted discounted_fare
         * or pricing was not applied.
         */
        @Nullable
        @ColumnInfo(name = "discounted_fare")
        public Double discountedFare;

        @Nullable
        @ColumnInfo(name = "bus_details_json")
        public String busDetailsJson;

        @ColumnInfo(name = "created_at")
        public long createdAt;

        public BookingDetailsEntity(
                @NonNull String userId,
                int bookingId,
                @Nullable String message,
                double startLat,
                double startLng,
                double endLat,
                double endLng,
                @Nullable String destinationsJson,
                int fare,
                @Nullable Double discountedFare,
                @Nullable String busDetailsJson,
                long createdAt
        ) {
            this.userId = userId;
            this.bookingId = bookingId;
            this.message = message;
            this.startLat = startLat;
            this.startLng = startLng;
            this.endLat = endLat;
            this.endLng = endLng;
            this.destinationsJson = destinationsJson;
            this.fare = fare;
            this.discountedFare = discountedFare;
            this.busDetailsJson = busDetailsJson;
            this.createdAt = createdAt;
        }
    }

    @Dao
    public interface UserProfileDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void saveUserProfile(UserProfileEntity userProfile);

        @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
        UserProfileEntity getUserProfile();

        @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
        UserProfileEntity getUserProfileByUserId(String userId);

        @Query("SELECT user_role FROM user_profile WHERE id = 1 LIMIT 1")
        String getUserRole();

        @Query("DELETE FROM user_profile")
        void clearUserProfile();
    }

    @Dao
    public interface SyncMetadataDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void saveSyncMetadata(SyncMetadataEntity syncMetadata);

        @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
        SyncMetadataEntity getSyncMetadata();

        @Query("DELETE FROM sync_metadata")
        void clearSyncMetadata();
    }
    @Dao
    public interface RouteDestinationDao {

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void saveRouteDestination(RouteDestination routeDestination);

        @Query("SELECT * FROM route_destinations WHERE route = :route AND destination = :destination LIMIT 1")
        RouteDestination getRouteDestination(String route, String destination);

        @Query("DELETE FROM route_destinations")
        void clearRouteDestinations();
    }

    @Dao
    public interface BookingDetailsDao {

        @Insert
        long insertBooking(BookingDetailsEntity booking);

        @Query("DELETE FROM booking_details WHERE user_id = :userId AND booking_id = :bookingId")
        void deleteBooking(String userId, int bookingId);

        @Transaction
        default long saveBookingForUser(BookingDetailsEntity booking) {
            deleteBooking(booking.userId, booking.bookingId);
            return insertBooking(booking);
        }

        @Query("SELECT * FROM booking_details WHERE user_id = :userId ORDER BY created_at DESC")
        java.util.List<BookingDetailsEntity> getBookingsForUser(String userId);

        /** Latest booking row for this user (coordinates live on booking_details, not user_profile). */
        @Query("SELECT * FROM booking_details WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
        BookingDetailsEntity getLatestBookingForUser(String userId);

        @Query("SELECT * FROM booking_details WHERE user_id = :userId AND booking_id = :bookingId LIMIT 1")
        BookingDetailsEntity getBooking(String userId, int bookingId);

        @Query("DELETE FROM booking_details WHERE user_id = :userId")
        void clearBookingsForUser(String userId);
    }
}
