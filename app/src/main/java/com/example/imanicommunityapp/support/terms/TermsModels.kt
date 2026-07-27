package com.example.imanicommunityapp.support.terms

import com.google.gson.annotations.SerializedName

data class TermsDocumentDto(
    @SerializedName("document_type") val documentType: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("body_format") val bodyFormat: String? = null,
    @SerializedName("effective_at") val effectiveAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

data class TermsApiEnvelope<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
)

data class TermsStatusDto(
    @SerializedName("document_type") val documentType: String? = null,
    @SerializedName("current_version") val currentVersion: String? = null,
    @SerializedName("accepted_version") val acceptedVersion: String? = null,
    @SerializedName("must_accept") val mustAccept: Boolean? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
)

data class TermsAcceptRequest(
    @SerializedName("version") val version: String,
    @SerializedName("locale") val locale: String,
    @SerializedName("document_type") val documentType: String = "TERMS",
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("app_version") val appVersion: String = "1.0",
)

data class TermsAcceptanceDto(
    @SerializedName("document_type") val documentType: String? = null,
    @SerializedName("version") val version: String? = null,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("platform") val platform: String? = null,
    @SerializedName("app_version") val appVersion: String? = null,
)
