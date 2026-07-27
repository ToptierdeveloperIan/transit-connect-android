package com.example.imanicommunityapp.settings;

import com.google.gson.annotations.SerializedName;

/**
 * DTOs for /api/settings/* (UserSettings backend).
 * Kept as simple POJOs for Retrofit/Gson; no business logic here.
 */
public final class SettingsModels {

    private SettingsModels() {
    }

    /** Standard API envelope used by UserSettings and Support. */
    public static class Envelope<T> {
        @SerializedName("success")
        public Boolean success;
        @SerializedName("message")
        public String message;
        @SerializedName("error")
        public String error;
        @SerializedName("data")
        public T data;
    }

    public static class ProfileSnapshot {
        @SerializedName("user_id")
        public Integer userId;
        @SerializedName("first_name")
        public String firstName;
        @SerializedName("second_name")
        public String secondName;
        @SerializedName("phone_number")
        public String phoneNumber;
        @SerializedName("email")
        public String email;
        @SerializedName("is_driver")
        public Boolean isDriver;
        @SerializedName("profile_version")
        public Integer profileVersion;
        @SerializedName("mutation_id")
        public String mutationId;
        @SerializedName("updated_at")
        public String updatedAt;
    }

    public static class UpdateNameRequest {
        @SerializedName("first_name")
        public final String firstName;
        @SerializedName("second_name")
        public final String secondName;
        @SerializedName("mutation_id")
        public final String mutationId;
        @SerializedName("base_version")
        public final Integer baseVersion;

        public UpdateNameRequest(String firstName, String secondName, String mutationId, Integer baseVersion) {
            this.firstName = firstName;
            this.secondName = secondName;
            this.mutationId = mutationId;
            this.baseVersion = baseVersion;
        }
    }

    public static class PhoneRequestBody {
        @SerializedName("new_phone_number")
        public final String newPhoneNumber;
        @SerializedName("mutation_id")
        public final String mutationId;

        public PhoneRequestBody(String newPhoneNumber, String mutationId) {
            this.newPhoneNumber = newPhoneNumber;
            this.mutationId = mutationId;
        }
    }

    public static class PhoneRequestResult {
        @SerializedName("challenge_id")
        public String challengeId;
        @SerializedName("expires_in")
        public Integer expiresIn;
        @SerializedName("masked_destination")
        public String maskedDestination;
        @SerializedName("mutation_id")
        public String mutationId;
    }

    public static class PhoneConfirmBody {
        @SerializedName("challenge_id")
        public final String challengeId;
        @SerializedName("otp")
        public final String otp;
        @SerializedName("mutation_id")
        public final String mutationId;

        public PhoneConfirmBody(String challengeId, String otp, String mutationId) {
            this.challengeId = challengeId;
            this.otp = otp;
            this.mutationId = mutationId;
        }
    }
}
