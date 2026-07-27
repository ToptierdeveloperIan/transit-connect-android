package com.example.imanicommunityapp.wallet

import com.google.gson.annotations.SerializedName

/**
 * DTOs for backend [Wallet] at `/api/wallet/` (see ridehaiingbackend/Wallet/WALLET.md).
 *
 * Envelope shape matches other Imani APIs:
 * `{ "success": true, "message": "...", "data": ... }`.
 */
data class WalletApiEnvelope<T>(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
)

/** GET /api/wallet/balance/ */
data class WalletBalanceDto(
    @SerializedName("wallet_id") val walletId: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("available_balance") val availableBalance: String? = null,
    @SerializedName("held_balance") val heldBalance: String? = null,
    @SerializedName("spendable") val spendable: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
)

/** GET /api/wallet/ledger/ item */
data class WalletLedgerEntryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("entry_type") val entryType: String? = null,
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("signed_amount") val signedAmount: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("provider_reference") val providerReference: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("balance_after") val balanceAfter: String? = null,
    @SerializedName("intent_id") val intentId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

/** GET /api/wallet/intents/ item */
data class WalletIntentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("provider_reference") val providerReference: String? = null,
    @SerializedName("fare_quote_id") val fareQuoteId: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("succeeded_at") val succeededAt: String? = null,
)

/** POST /api/wallet/deposits/ body */
data class WalletDepositRequest(
    @SerializedName("amount") val amount: String,
    @SerializedName("channel") val channel: String,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("description") val description: String = "",
)

/** POST /api/wallet/deposits/ data */
data class WalletDepositResultDto(
    @SerializedName("intent_id") val intentId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("channel") val channel: String? = null,
    @SerializedName("idempotency_key") val idempotencyKey: String? = null,
    @SerializedName("note") val note: String? = null,
)

/** POST /api/wallet/spend/ body — amount is server-side from fare quote. */
data class WalletSpendRequest(
    @SerializedName("quote_id") val quoteId: String,
    @SerializedName("idempotency_key") val idempotencyKey: String,
    @SerializedName("booking_id") val bookingId: Int? = null,
    @SerializedName("description") val description: String = "",
)
