package com.checkout.mobilechallenge.albertomanzano.domain.usecase

import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Use case for retrieving payment details and status.
 * Used to fetch detailed error information after 3DS failure.
 */
class GetPaymentDetailsUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    /**
     * Executes the payment details retrieval.
     *
     * @param paymentId The payment ID to retrieve details for
     * @return Result containing PaymentResult with current payment status and error information
     */
    suspend operator fun invoke(paymentId: String): Result<PaymentResult> =
        paymentRepository.getPaymentDetails(paymentId)
}

