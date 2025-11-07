package com.checkout.mobilechallenge.albertomanzano.ui.payment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.usecase.GetPaymentDetailsUseCase
import com.checkout.mobilechallenge.albertomanzano.domain.usecase.ProcessPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * UI state for the payment flow screen.
 *
 * @param isLoading Whether a payment operation is in progress
 * @param error Error message to display to the user
 * @param paymentResult The result of the payment operation (Success, Failure, or Pending)
 * @param redirectUrl The 3DS redirect URL if payment is pending
 * @param paymentId The payment ID to retrieve details after 3DS completion
 * @param paymentErrorBefore3DS Error message from payment response before 3DS redirect
 * @param isNetworkError True when the error is due to network connectivity issues
 */
data class PaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val paymentResult: PaymentResult? = null,
    val redirectUrl: String? = null,
    val paymentId: String? = null,
    val paymentErrorBefore3DS: String? = null,
    val isNetworkError: Boolean = false
)

/**
 * ViewModel for managing payment flow UI state.
 * Delegates business logic to use cases following clean architecture principles.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val processPaymentUseCase: ProcessPaymentUseCase,
    private val getPaymentDetailsUseCase: GetPaymentDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // Store last payment attempt for retry functionality
    private var lastCardDetails: CardDetails? = null
    private var lastAmount: Int = 6540

    /**
     * Processes a payment by delegating to the ProcessPaymentUseCase.
     * Updates UI state based on the payment result.
     *
     * @param cardDetails The card details to process
     * @param amount The payment amount in pence (default: 6540 = £65.40)
     */
    fun processPayment(cardDetails: CardDetails, amount: Int = 6540) {
        // Store details for potential retry
        lastCardDetails = cardDetails
        lastAmount = amount

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isNetworkError = false)

            val paymentResult = processPaymentUseCase(cardDetails, amount)
            
            paymentResult.fold(
                onSuccess = { result ->
                    _uiState.value = when (result) {
                        is PaymentResult.Pending -> {
                            _uiState.value.copy(
                                isLoading = false,
                                paymentResult = result,
                                redirectUrl = result.redirectUrl,
                                paymentId = result.paymentId,
                                paymentErrorBefore3DS = null,
                                isNetworkError = false
                            )
                        }
                        is PaymentResult.Success -> {
                            _uiState.value.copy(
                                isLoading = false,
                                paymentResult = result,
                                isNetworkError = false
                            )
                        }
                        is PaymentResult.Failure -> {
                            _uiState.value.copy(
                                isLoading = false,
                                paymentResult = result,
                                error = result.message,
                                paymentErrorBefore3DS = result.message,
                                isNetworkError = false
                            )
                        }
                    }
                },
                onFailure = { error ->
                    Log.e("PaymentFlow", "Payment processing failed", error)

                    // Detect network errors
                    val isNetworkError = error is IOException || error is UnknownHostException
                    val errorMessage = if (isNetworkError) {
                        "No internet connection. Please check your network and try again."
                    } else {
                        error.message ?: "Payment processing failed"
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        paymentResult = PaymentResult.Failure(errorMessage),
                        error = errorMessage,
                        isNetworkError = isNetworkError
                    )
                }
            )
        }
    }

    /**
     * Retries the last payment attempt using stored card details.
     * Used when recovering from network errors.
     */
    fun retryPayment() {
        lastCardDetails?.let { cardDetails ->
            processPayment(cardDetails, lastAmount)
        }
    }

    /**
     * Clears the current error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, isNetworkError = false)
    }

    /**
     * Clears the payment result and all related state.
     * Used when returning to the card input screen.
     */
    fun clearPaymentResult() {
        _uiState.value = _uiState.value.copy(
            paymentResult = null, 
            redirectUrl = null,
            paymentId = null,
            paymentErrorBefore3DS = null,
            isNetworkError = false
        )
    }
    
    /**
     * Gets the error message stored before 3DS redirect.
     * Used to display error details after 3DS failure.
     *
     * @return The error message if available, null otherwise
     */
    fun getPaymentErrorBefore3DS(): String? = _uiState.value.paymentErrorBefore3DS
    
    /**
     * Fetches payment details after a 3DS failure to retrieve specific error information.
     * Updates the UI state with the detailed error message.
     *
     * @param paymentId The payment ID to fetch details for
     */
    suspend fun fetchPaymentDetailsAfter3DSFailure(paymentId: String) {
        viewModelScope.launch {
            val result = getPaymentDetailsUseCase(paymentId)
            result.fold(
                onSuccess = { paymentResult ->
                    if (paymentResult is PaymentResult.Failure) {
                        _uiState.value = _uiState.value.copy(
                            paymentErrorBefore3DS = paymentResult.message
                        )
                    }
                },
                onFailure = { error ->
                    Log.e("PaymentFlow", "Failed to fetch payment details after 3DS failure", error)
                    // If we can't fetch details, keep existing error
                }
            )
        }
    }
}

