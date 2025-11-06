package com.checkout.mobilechallenge.albertomanzano.domain.model

/**
 * Sealed class representing the result of a payment operation.
 * Used to model different payment outcomes in the domain layer.
 */
sealed class PaymentResult {
    /**
     * Represents a successful payment.
     *
     * @param message Success message to display to the user
     */
    data class Success(val message: String = "Payment successful!") : PaymentResult()
    
    /**
     * Represents a failed payment.
     *
     * @param message Error message describing the failure reason
     */
    data class Failure(val message: String = "Payment failed") : PaymentResult()
    
    /**
     * Represents a payment that requires 3D Secure authentication.
     *
     * @param redirectUrl The URL to redirect to for 3DS verification
     * @param paymentId The payment ID to retrieve details after 3DS completion
     */
    data class Pending(val redirectUrl: String, val paymentId: String? = null) : PaymentResult()
}

