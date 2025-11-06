package com.checkout.mobilechallenge.albertomanzano.domain.usecase

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Use case for processing a payment.
 * Orchestrates the complete payment flow: tokenization followed by payment processing.
 */
class ProcessPaymentUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    /**
     * Executes the complete payment process:
     * 1. Tokenizes the card details
     * 2. Processes the payment with the token
     *
     * @param cardDetails The card details to process
     * @param amount The payment amount in pence (e.g., 6540 for £65.40)
     * @return Result containing PaymentResult (Success, Failure, or Pending with 3DS redirect URL)
     */
    suspend operator fun invoke(
        cardDetails: CardDetails,
        amount: Int = 6540
    ): Result<PaymentResult> {
        // Step 1: Tokenize card
        val tokenResult = paymentRepository.tokenizeCard(cardDetails)
        
        return tokenResult.fold(
            onSuccess = { token ->
                // Step 2: Process payment with token
                paymentRepository.processPayment(token, amount)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}

