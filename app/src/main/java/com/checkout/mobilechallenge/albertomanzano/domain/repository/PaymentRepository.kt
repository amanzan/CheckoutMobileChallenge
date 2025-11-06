package com.checkout.mobilechallenge.albertomanzano.domain.repository

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult

/**
 * Repository interface for payment-related operations.
 * Provides abstraction for payment processing, tokenization, and payment status retrieval.
 */
interface PaymentRepository {
    /**
     * Tokenizes card details by sending them to the payment provider API.
     *
     * @param cardDetails The card details to tokenize
     * @return Result containing the token string on success, or an exception on failure
     */
    suspend fun tokenizeCard(cardDetails: CardDetails): Result<String>
    
    /**
     * Processes a payment using a tokenized card.
     *
     * @param token The tokenized card token
     * @param amount The payment amount in pence (e.g., 6540 for £65.40)
     * @return Result containing PaymentResult (Success, Failure, or Pending with 3DS redirect URL)
     */
    suspend fun processPayment(token: String, amount: Int): Result<PaymentResult>
    
    /**
     * Retrieves payment details and status for a given payment ID.
     * Used to fetch error details after 3DS failure.
     *
     * @param paymentId The payment ID to retrieve details for
     * @return Result containing PaymentResult with current payment status and error information
     */
    suspend fun getPaymentDetails(paymentId: String): Result<PaymentResult>
}

