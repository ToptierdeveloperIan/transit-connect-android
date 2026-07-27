package com.example.imanicommunityapp.wallet

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Backend Wallet surface under `/api/wallet/`
 * (Retrofit base URL already includes `/api/`).
 *
 * Contract: ridehaiingbackend/Wallet/WALLET.md
 */
interface WalletApi {

    @GET("wallet/health/")
    fun health(): Call<Map<String, Any>>

    @GET("wallet/balance/")
    fun getBalance(): Call<WalletApiEnvelope<WalletBalanceDto>>

    @GET("wallet/ledger/")
    fun getLedger(@Query("limit") limit: Int = 50): Call<WalletApiEnvelope<List<WalletLedgerEntryDto>>>

    @GET("wallet/intents/")
    fun getIntents(@Query("limit") limit: Int = 30): Call<WalletApiEnvelope<List<WalletIntentDto>>>

    @POST("wallet/deposits/")
    fun createDeposit(@Body body: WalletDepositRequest): Call<WalletApiEnvelope<WalletDepositResultDto>>

    @POST("wallet/spend/")
    fun spend(@Body body: WalletSpendRequest): Call<WalletApiEnvelope<Map<String, Any>>>
}
