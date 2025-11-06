package com.checkout.mobilechallenge.albertomanzano.domain.usecase

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Use case for tokenizing card details.
 * Encapsulates the business logic for converting card details into a secure token.
 */
class TokenizeCardUseCase @Inject constructor(
    private val paymentRepository: PaymentRepository
) {
    /**
     * Executes the card tokenization process.
     *
     * @param cardDetails The card details to tokenize
     * @return Result containing the token string on success, or an exception on failure
     */
    suspend operator fun invoke(cardDetails: CardDetails) = 
        paymentRepository.tokenizeCard(cardDetails)
}

