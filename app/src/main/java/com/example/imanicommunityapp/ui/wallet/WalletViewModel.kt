package com.example.imanicommunityapp.ui.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imanicommunityapp.wallet.WalletBalanceDto
import com.example.imanicommunityapp.wallet.WalletDepositResultDto
import com.example.imanicommunityapp.wallet.WalletIntentDto
import com.example.imanicommunityapp.wallet.WalletLedgerEntryDto
import com.example.imanicommunityapp.wallet.WalletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Deposit rail shown in the top-up sheet. */
enum class DepositChannel(val apiValue: String, val label: String) {
    MPESA("MPESA", "M-Pesa"),
    AIRTEL("AIRTEL", "Airtel Money"),
}

data class WalletUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isDepositing: Boolean = false,
    val balance: WalletBalanceDto? = null,
    val ledger: List<WalletLedgerEntryDto> = emptyList(),
    val intents: List<WalletIntentDto> = emptyList(),
    val error: String? = null,
    val depositMessage: String? = null,
    val depositError: String? = null,
    val showDepositSheet: Boolean = false,
    val depositAmount: String = "",
    val depositChannel: DepositChannel = DepositChannel.MPESA,
)

/**
 * Loads balance + ledger + intents from [WalletRepository] and handles deposit intents.
 *
 * Spend (pay fare from wallet) is intentionaly not driven from this screen —
 * it needs a checkout [quote_id]; use the booking payment path later.
 */
class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WalletRepository(application)

    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    fun refresh(showFullLoading: Boolean = true) {
        viewModelScope.launch {
            if (showFullLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            } else {
                _state.update { it.copy(isRefreshing = true, error = null) }
            }

            val balanceResult = fetchBalanceSuspend()
            val ledgerResult = fetchLedgerSuspend()
            val intentsResult = fetchIntentsSuspend()

            val firstError = balanceResult.exceptionOrNull()?.message
                ?: ledgerResult.exceptionOrNull()?.message
                ?: intentsResult.exceptionOrNull()?.message

            _state.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    balance = balanceResult.getOrNull() ?: it.balance,
                    ledger = ledgerResult.getOrNull() ?: it.ledger,
                    intents = intentsResult.getOrNull() ?: it.intents,
                    error = if (balanceResult.isFailure) firstError else null,
                )
            }
        }
    }

    fun openDepositSheet() {
        _state.update {
            it.copy(
                showDepositSheet = true,
                depositError = null,
                depositMessage = null,
            )
        }
    }

    fun dismissDepositSheet() {
        _state.update {
            it.copy(
                showDepositSheet = false,
                depositError = null,
            )
        }
    }

    fun setDepositAmount(amount: String) {
        // Allow digits and one decimal point only.
        val filtered = amount.filter { it.isDigit() || it == '.' }
        _state.update { it.copy(depositAmount = filtered, depositError = null) }
    }

    fun setDepositChannel(channel: DepositChannel) {
        _state.update { it.copy(depositChannel = channel) }
    }

    fun clearDepositFeedback() {
        _state.update { it.copy(depositMessage = null, depositError = null) }
    }

    fun submitDeposit() {
        val amount = _state.value.depositAmount.trim()
        val amountValue = amount.toDoubleOrNull()
        if (amountValue == null || amountValue < 1.0) {
            _state.update { it.copy(depositError = "Enter at least KES 1") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isDepositing = true, depositError = null, depositMessage = null) }
            val result = createDepositSuspend(
                amount = amount,
                channel = _state.value.depositChannel.apiValue,
            )
            result.fold(
                onSuccess = { data ->
                    _state.update {
                        it.copy(
                            isDepositing = false,
                            showDepositSheet = false,
                            depositAmount = "",
                            depositMessage = buildDepositSuccessMessage(data),
                        )
                    }
                    // Refresh intents (and balance if already credited by recon).
                    refresh(showFullLoading = false)
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isDepositing = false,
                            depositError = err.message ?: "Deposit failed",
                        )
                    }
                },
            )
        }
    }

    private fun buildDepositSuccessMessage(data: WalletDepositResultDto): String {
        val amount = data.amount ?: _state.value.depositAmount
        val channel = data.channel ?: _state.value.depositChannel.label
        val status = data.status ?: "PENDING_PROVIDER"
        return "Top-up of KES $amount via $channel recorded ($status). " +
            "Balance updates after M-Pesa/Airtel confirms payment."
    }

    private suspend fun fetchBalanceSuspend(): Result<WalletBalanceDto> =
        suspendCancellableCoroutine { cont ->
            repository.fetchBalance(object : WalletRepository.WalletCallback<WalletBalanceDto> {
                override fun onSuccess(data: WalletBalanceDto) {
                    if (cont.isActive) cont.resume(Result.success(data))
                }

                override fun onError(message: String) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                }
            })
        }

    private suspend fun fetchLedgerSuspend(): Result<List<WalletLedgerEntryDto>> =
        suspendCancellableCoroutine { cont ->
            repository.fetchLedger(50, object : WalletRepository.WalletCallback<List<WalletLedgerEntryDto>> {
                override fun onSuccess(data: List<WalletLedgerEntryDto>) {
                    if (cont.isActive) cont.resume(Result.success(data))
                }

                override fun onError(message: String) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                }
            })
        }

    private suspend fun fetchIntentsSuspend(): Result<List<WalletIntentDto>> =
        suspendCancellableCoroutine { cont ->
            repository.fetchIntents(20, object : WalletRepository.WalletCallback<List<WalletIntentDto>> {
                override fun onSuccess(data: List<WalletIntentDto>) {
                    if (cont.isActive) cont.resume(Result.success(data))
                }

                override fun onError(message: String) {
                    if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                }
            })
        }

    private suspend fun createDepositSuspend(
        amount: String,
        channel: String,
    ): Result<WalletDepositResultDto> =
        suspendCancellableCoroutine { cont ->
            repository.createDeposit(
                amount = amount,
                channel = channel,
                callback = object : WalletRepository.WalletCallback<WalletDepositResultDto> {
                    override fun onSuccess(data: WalletDepositResultDto) {
                        if (cont.isActive) cont.resume(Result.success(data))
                    }

                    override fun onError(message: String) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }
}
