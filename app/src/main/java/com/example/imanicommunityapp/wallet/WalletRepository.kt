package com.example.imanicommunityapp.wallet

import android.content.Context
import com.example.imanicommunityapp.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

/**
 * Authenticated access to the backend Wallet app (`/api/wallet/`).
 *
 * Deposit notes:
 * - POST deposits only creates an intent (PENDING_PROVIDER); ledger credit
 *   happens after provider success on the backend (STK callback wiring).
 * - Spend requires a server-side open FareQuote (`quote_id`); amount is never
 *   taken from the client.
 */
class WalletRepository(context: Context) {

    private val api: WalletApi = RetrofitClient
        .getClient(context.applicationContext)
        .create(WalletApi::class.java)

    fun fetchBalance(callback: WalletCallback<WalletBalanceDto>) {
        api.getBalance().enqueue(envelopeCallback(callback))
    }

    fun fetchLedger(limit: Int = 50, callback: WalletCallback<List<WalletLedgerEntryDto>>) {
        api.getLedger(limit).enqueue(envelopeCallback(callback))
    }

    fun fetchIntents(limit: Int = 30, callback: WalletCallback<List<WalletIntentDto>>) {
        api.getIntents(limit).enqueue(envelopeCallback(callback))
    }

    fun createDeposit(
        amount: String,
        channel: String,
        description: String = "Wallet top-up",
        callback: WalletCallback<WalletDepositResultDto>,
    ) {
        val body = WalletDepositRequest(
            amount = amount,
            channel = channel.uppercase(),
            idempotencyKey = UUID.randomUUID().toString(),
            description = description,
        )
        api.createDeposit(body).enqueue(envelopeCallback(callback))
    }

    fun spend(
        quoteId: String,
        bookingId: Int? = null,
        callback: WalletCallback<Map<String, Any>>,
    ) {
        val body = WalletSpendRequest(
            quoteId = quoteId,
            idempotencyKey = UUID.randomUUID().toString(),
            bookingId = bookingId,
            description = "Pay fare from wallet",
        )
        api.spend(body).enqueue(envelopeCallback(callback))
    }

    private fun <T> envelopeCallback(callback: WalletCallback<T>): Callback<WalletApiEnvelope<T>> {
        return object : Callback<WalletApiEnvelope<T>> {
            override fun onResponse(
                call: Call<WalletApiEnvelope<T>>,
                response: Response<WalletApiEnvelope<T>>,
            ) {
                val envelope = response.body()
                if (response.isSuccessful && envelope?.success == true && envelope.data != null) {
                    callback.onSuccess(envelope.data)
                } else {
                    val msg = envelope?.message
                        ?: response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                        ?: "Request failed (${response.code()})"
                    callback.onError(msg)
                }
            }

            override fun onFailure(call: Call<WalletApiEnvelope<T>>, t: Throwable) {
                callback.onError(t.message ?: "Network error")
            }
        }
    }

    interface WalletCallback<T> {
        fun onSuccess(data: T)
        fun onError(message: String)
    }
}
