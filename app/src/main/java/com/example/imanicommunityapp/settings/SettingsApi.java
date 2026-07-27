package com.example.imanicommunityapp.settings;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;

/**
 * Retrofit contract for UserSettings backend (base URL ends with /api/).
 *
 * @see docs/PROFILE_SETTINGS_SYNC.md
 */
public interface SettingsApi {

    @GET("settings/profile/")
    Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> getProfile();

    /**
     * Name update — used for online save and offline queue replay.
     * Same endpoint keeps a single server writer.
     */
    @PATCH("settings/profile/name/")
    Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> updateName(
            @Body SettingsModels.UpdateNameRequest body
    );

    /** Step 1: OTP to the NEW phone. Does not commit identity. */
    @POST("settings/profile/phone/request/")
    Call<SettingsModels.Envelope<SettingsModels.PhoneRequestResult>> requestPhoneChange(
            @Body SettingsModels.PhoneRequestBody body
    );

    /** Step 2: OTP confirm — only then is phone_number authoritative. */
    @POST("settings/profile/phone/confirm/")
    Call<SettingsModels.Envelope<SettingsModels.ProfileSnapshot>> confirmPhoneChange(
            @Body SettingsModels.PhoneConfirmBody body
    );
}
