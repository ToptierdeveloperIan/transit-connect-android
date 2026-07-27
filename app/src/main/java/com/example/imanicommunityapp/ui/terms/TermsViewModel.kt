package com.example.imanicommunityapp.ui.terms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imanicommunityapp.support.terms.TermsDocumentDto
import com.example.imanicommunityapp.support.terms.TermsRepository
import com.example.imanicommunityapp.support.terms.TermsStatusDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class TermsLocale(val code: String, val labelEn: String, val labelNative: String) {
    EN("en", "English", "English"),
    SW("sw", "Kiswahili", "Kiswahili"),
}

data class TermsUiState(
    val locale: TermsLocale = TermsLocale.EN,
    val isLoading: Boolean = true,
    val isAccepting: Boolean = false,
    val document: TermsDocumentDto? = null,
    val status: TermsStatusDto? = null,
    val error: String? = null,
    val acceptError: String? = null,
    val acceptedJustNow: Boolean = false,
    val requireAccept: Boolean = false,
)

class TermsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TermsRepository(application)

    private val _state = MutableStateFlow(TermsUiState())
    val state: StateFlow<TermsUiState> = _state.asStateFlow()

    fun setRequireAccept(require: Boolean) {
        _state.update { it.copy(requireAccept = require) }
    }

    fun bootstrap(requireAccept: Boolean) {
        _state.update { it.copy(requireAccept = requireAccept) }
        refresh()
    }

    fun selectLocale(locale: TermsLocale) {
        if (_state.value.locale == locale) return
        _state.update { it.copy(locale = locale) }
        loadDocument()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadStatusInternal()
            loadDocumentInternal()
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun accept(onAccepted: () -> Unit) {
        val doc = _state.value.document
        val version = doc?.version
        if (version.isNullOrBlank()) {
            _state.update { it.copy(acceptError = "Terms version unavailable") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isAccepting = true, acceptError = null) }
            val result = acceptSuspend(version, _state.value.locale.code)
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isAccepting = false,
                            acceptedJustNow = true,
                            status = it.status?.copy(
                                mustAccept = false,
                                acceptedVersion = version,
                            ) ?: TermsStatusDto(
                                mustAccept = false,
                                currentVersion = version,
                                acceptedVersion = version,
                            ),
                        )
                    }
                    onAccepted()
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            isAccepting = false,
                            acceptError = err.message ?: "Accept failed",
                        )
                    }
                },
            )
        }
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadDocumentInternal()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadDocumentInternal() {
        val locale = _state.value.locale.code
        val result = fetchTermsSuspend(locale)
        result.fold(
            onSuccess = { doc -> _state.update { it.copy(document = doc, error = null) } },
            onFailure = { err -> _state.update { it.copy(error = err.message, document = null) } },
        )
    }

    private suspend fun loadStatusInternal() {
        val result = fetchStatusSuspend()
        result.fold(
            onSuccess = { status -> _state.update { it.copy(status = status) } },
            onFailure = {
                // Unauthenticated settings view still works without status
                _state.update { it.copy(status = null) }
            },
        )
    }

    private suspend fun fetchTermsSuspend(locale: String): Result<TermsDocumentDto> =
        suspendCancellableCoroutine { cont ->
            repository.fetchTerms(
                locale,
                object : TermsRepository.TermsCallback<TermsDocumentDto> {
                    override fun onSuccess(data: TermsDocumentDto) {
                        if (cont.isActive) cont.resume(Result.success(data))
                    }

                    override fun onError(message: String) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }

    private suspend fun fetchStatusSuspend(): Result<TermsStatusDto> =
        suspendCancellableCoroutine { cont ->
            repository.fetchStatus(
                object : TermsRepository.TermsCallback<TermsStatusDto> {
                    override fun onSuccess(data: TermsStatusDto) {
                        if (cont.isActive) cont.resume(Result.success(data))
                    }

                    override fun onError(message: String) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }

    private suspend fun acceptSuspend(version: String, locale: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            repository.accept(
                version,
                locale,
                object : TermsRepository.TermsCallback<com.example.imanicommunityapp.support.terms.TermsAcceptanceDto> {
                    override fun onSuccess(data: com.example.imanicommunityapp.support.terms.TermsAcceptanceDto) {
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }

                    override fun onError(message: String) {
                        if (cont.isActive) cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }
}
