package com.example.imanicommunityapp.support.terms

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Backend Support legal surface under /api/support/
 * (base URL already includes /api/).
 */
interface TermsApi {

    @GET("support/terms/")
    fun getCurrentTerms(@Query("locale") locale: String): Call<TermsApiEnvelope<TermsDocumentDto>>

    @GET("support/terms/status/")
    fun getTermsStatus(): Call<TermsApiEnvelope<TermsStatusDto>>

    @POST("support/terms/accept/")
    fun acceptTerms(@Body body: TermsAcceptRequest): Call<TermsApiEnvelope<TermsAcceptanceDto>>
}
