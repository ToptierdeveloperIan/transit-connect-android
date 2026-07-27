package com.example.imanicommunityapp.support.terms

import android.content.Context
import com.example.imanicommunityapp.auth.Repository.authRetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Terms network access.
 * Document fetch uses plain client (public). Status/accept use authenticated hub.
 */
class TermsRepository(context: Context) {

    private val appContext = context.applicationContext

    private val publicApi: TermsApi by lazy {
        authRetrofitClient.getPlainClient().create(TermsApi::class.java)
    }

    private val authedApi: TermsApi by lazy {
        authRetrofitClient.getClient(appContext).create(TermsApi::class.java)
    }

    fun fetchTerms(locale: String, callback: TermsCallback<TermsDocumentDto>) {
        publicApi.getCurrentTerms(locale).enqueue(envelopeCallback(callback))
    }

    fun fetchStatus(callback: TermsCallback<TermsStatusDto>) {
        authedApi.getTermsStatus().enqueue(envelopeCallback(callback))
    }

    fun accept(version: String, locale: String, callback: TermsCallback<TermsAcceptanceDto>) {
        val body = TermsAcceptRequest(version = version, locale = locale)
        authedApi.acceptTerms(body).enqueue(envelopeCallback(callback))
    }

    /**
     * Blocking-style helper for Java call sites that already run off the main path
     * with a short splash delay. Prefer [fetchStatus] from Kotlin UI.
     */
    fun fetchStatusSync(): Result<TermsStatusDto> {
        return try {
            val response = authedApi.getTermsStatus().execute()
            val envelope = response.body()
            if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
                Result.success(envelope.data)
            } else {
                Result.failure(IllegalStateException(envelope?.message ?: "status_failed"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun <T> envelopeCallback(callback: TermsCallback<T>): Callback<TermsApiEnvelope<T>> {
        return object : Callback<TermsApiEnvelope<T>> {
            override fun onResponse(
                call: Call<TermsApiEnvelope<T>>,
                response: Response<TermsApiEnvelope<T>>,
            ) {
                val envelope = response.body()
                if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
                    callback.onSuccess(envelope.data)
                } else {
                    val msg = envelope?.message
                        ?: response.errorBody()?.string()
                        ?: "Request failed (${response.code()})"
                    callback.onError(msg)
                }
            }

            override fun onFailure(call: Call<TermsApiEnvelope<T>>, t: Throwable) {
                callback.onError(t.message ?: "Network error")
            }
        }
    }

    interface TermsCallback<T> {
        fun onSuccess(data: T)
        fun onError(message: String)
    }
}
