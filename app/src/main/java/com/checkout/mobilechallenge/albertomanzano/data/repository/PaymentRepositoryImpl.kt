package com.checkout.mobilechallenge.albertomanzano.data.repository

import com.checkout.mobilechallenge.albertomanzano.data.remote.api.CheckoutApi
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentRequestDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentResponseDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentSourceDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.ThreeDsDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.TokenRequestDto
import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import android.util.Log
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class PaymentRepositoryImpl @Inject constructor(
    @Named("checkout_token_api") private val tokenApi: CheckoutApi,
    @Named("checkout_payment_api") private val paymentApi: CheckoutApi
) : PaymentRepository {

    companion object {
        private const val SUCCESS_URL = "https://example.com/payments/success"
        private const val FAILURE_URL = "https://example.com/payments/fail"
    }

    override suspend fun tokenizeCard(cardDetails: CardDetails): Result<String> {
        return try {
            val request = TokenRequestDto(
                number = cardDetails.number,
                expiryMonth = cardDetails.expiryMonth,
                expiryYear = cardDetails.expiryYear,
                cvv = cardDetails.cvv
            )
            val response = tokenApi.requestToken(request)
            Result.success(response.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processPayment(token: String, amount: Int): Result<PaymentResult> {
        return try {
            val request = PaymentRequestDto(
                source = PaymentSourceDto(token = token),
                amount = amount,
                currency = "GBP",
                threeDs = ThreeDsDto(enabled = true),
                successUrl = SUCCESS_URL,
                failureUrl = FAILURE_URL
            )
            val response = paymentApi.requestPayment(request)
            
            // Store payment ID for later retrieval if 3DS fails
            val paymentId = response.id
            
            // Check if response has a status (successful response)
            when {
                response.status == "Pending" && response.links?.redirect?.href != null -> {
                    Result.success(PaymentResult.Pending(response.links.redirect.href, paymentId))
                }
                response.status == "Authorized" || response.status == "Captured" -> {
                    Result.success(PaymentResult.Success())
                }
                response.status != null -> {
                    // Status exists - check if it's a failure status
                    // Any status other than Pending, Authorized, or Captured is considered a failure
                    val isSuccess = response.status in listOf("Pending", "Authorized", "Captured")
                    
                    if (!isSuccess) {
                        // This is a failure - extract error message from response, including response code if available
                        val summary = response.responseSummary
                            ?: response.declineReason
                            ?: response.message
                        val errorMessage = if (response.responseCode != null && summary != null) {
                            "${response.responseCode}: $summary"
                        } else {
                            summary
                                ?: response.responseCode
                                ?: response.errorType
                                ?: response.errorCodes?.joinToString(", ")
                                ?: "Payment failed. Status: ${response.status}"
                        }
                        Result.success(PaymentResult.Failure(errorMessage))
                    } else {
                        // Should not reach here, but handle just in case
                        Result.success(PaymentResult.Failure("Unexpected payment status: ${response.status}"))
                    }
                }
                else -> {
                    // No status field - likely an error response
                    val errorMessage = response.responseSummary
                        ?: response.declineReason
                        ?: response.message
                        ?: response.responseCode
                        ?: response.errorType
                        ?: response.errorCodes?.joinToString(", ")
                        ?: "Payment failed"
                    Result.success(PaymentResult.Failure(errorMessage))
                }
            }
        } catch (e: HttpException) {
            // Handle HTTP errors (4xx, 5xx) - these indicate payment failures
            Log.e("PaymentFlow", "HTTP error in payment request: ${e.code()} - ${e.message}", e)
            val errorMessage = try {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    // Try to parse error response
                    val gson = Gson()
                    try {
                        val errorResponse = gson.fromJson(errorBody, PaymentResponseDto::class.java)
                        errorResponse.responseSummary
                            ?: errorResponse.declineReason
                            ?: errorResponse.message
                            ?: errorResponse.responseCode
                            ?: errorResponse.errorType
                            ?: errorResponse.errorCodes?.joinToString(", ")
                            ?: "Payment failed with status ${e.code()}"
                    } catch (parseException: Exception) {
                        Log.e("PaymentFlow", "Failed to parse error response", parseException)
                        // If parsing fails, try to extract any readable error message
                        if (errorBody.contains("\"message\"")) {
                            val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)
                            messageMatch?.groupValues?.get(1) ?: "Payment failed with status ${e.code()}"
                        } else {
                            "Payment failed with status ${e.code()}"
                        }
                    }
                } else {
                    "Payment failed with status ${e.code()}"
                }
            } catch (parseException: Exception) {
                Log.e("PaymentFlow", "Exception while processing error", parseException)
                "Payment failed: ${e.message ?: "Unknown error"}"
            }
            Result.success(PaymentResult.Failure(errorMessage))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPaymentDetails(paymentId: String): Result<PaymentResult> {
        return try {
            val response = paymentApi.getPaymentDetails(paymentId)
            
            // If payment details don't have error info, try fetching payment actions
            val hasErrorInfo = response.responseSummary != null || 
                              response.responseCode != null || 
                              response.declineReason != null ||
                              response.message != null
            
            var errorMessage: String? = null
            
            if (!hasErrorInfo && response.status != null && response.status !in listOf("Authorized", "Captured", "Pending")) {
                // Try to get error info from payment actions
                try {
                    val actionsResponse = paymentApi.getPaymentActions(paymentId)
                    
                    // Get the most recent action (usually the last one)
                    actionsResponse.lastOrNull()?.let { action ->
                        // Extract error message from action, including response code if available
                        val summary = action.responseSummary
                            ?: action.declineReason
                            ?: action.message
                        errorMessage = if (action.responseCode != null && summary != null) {
                            "${action.responseCode}: $summary"
                        } else {
                            summary ?: action.responseCode
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PaymentFlow", "Failed to fetch payment actions", e)
                }
            }
            
            when {
                response.status == "Authorized" || response.status == "Captured" -> {
                    Result.success(PaymentResult.Success())
                }
                response.status != null -> {
                    // Extract error message from response or actions, including response code if available
                    val finalErrorMessage = if (errorMessage != null) {
                        errorMessage // Already formatted with code from actions
                    } else {
                        val summary = response.responseSummary
                            ?: response.declineReason
                            ?: response.message
                        if (response.responseCode != null && summary != null) {
                            "${response.responseCode}: $summary"
                        } else {
                            summary
                                ?: response.responseCode
                                ?: response.errorType
                                ?: response.errorCodes?.joinToString(", ")
                                ?: "Payment failed. Status: ${response.status}"
                        }
                    }
                    Result.success(PaymentResult.Failure(finalErrorMessage))
                }
                else -> {
                    val finalErrorMessage = if (errorMessage != null) {
                        errorMessage // Already formatted with code from actions
                    } else {
                        val summary = response.responseSummary
                            ?: response.declineReason
                            ?: response.message
                        if (response.responseCode != null && summary != null) {
                            "${response.responseCode}: $summary"
                        } else {
                            summary
                                ?: response.responseCode
                                ?: response.errorType
                                ?: response.errorCodes?.joinToString(", ")
                                ?: "Payment failed"
                        }
                    }
                    Result.success(PaymentResult.Failure(finalErrorMessage))
                }
            }
        } catch (e: HttpException) {
            Log.e("PaymentFlow", "HTTP error getting payment details: ${e.code()} - ${e.message}", e)
            val errorMessage = try {
                val errorBody = e.response()?.errorBody()?.string()
                if (errorBody != null) {
                    val gson = Gson()
                    try {
                        val errorResponse = gson.fromJson(errorBody, PaymentResponseDto::class.java)
                        errorResponse.responseSummary
                            ?: errorResponse.declineReason
                            ?: errorResponse.message
                            ?: errorResponse.responseCode
                            ?: errorResponse.errorType
                            ?: errorResponse.errorCodes?.joinToString(", ")
                            ?: "Payment failed with status ${e.code()}"
                    } catch (parseException: Exception) {
                        Log.e("PaymentFlow", "Failed to parse error response", parseException)
                        "Payment failed with status ${e.code()}"
                    }
                } else {
                    "Payment failed with status ${e.code()}"
                }
            } catch (parseException: Exception) {
                Log.e("PaymentFlow", "Exception while processing error", parseException)
                "Payment failed: ${e.message ?: "Unknown error"}"
            }
            Result.success(PaymentResult.Failure(errorMessage))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

