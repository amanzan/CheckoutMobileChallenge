package com.checkout.mobilechallenge.albertomanzano.domain.usecase

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class ProcessPaymentUseCaseTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    private lateinit var processPaymentUseCase: ProcessPaymentUseCase

    @Before
    fun setup() {
        processPaymentUseCase = ProcessPaymentUseCase(paymentRepository)
    }

    @Test
    fun `invoke should return success when tokenization and payment succeed`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val amount = 6540
        val expectedResult = PaymentResult.Success()
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, amount)).thenReturn(Result.success(expectedResult))

        val result = processPaymentUseCase(cardDetails, amount)

        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        verify(paymentRepository).tokenizeCard(cardDetails)
        verify(paymentRepository).processPayment(token, amount)
    }

    @Test
    fun `invoke should return pending result when payment requires 3DS`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val amount = 6540
        val redirectUrl = "https://api.checkout.com/3ds/test"
        val paymentId = "pay_test123"
        val expectedResult = PaymentResult.Pending(redirectUrl, paymentId)
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, amount)).thenReturn(Result.success(expectedResult))

        val result = processPaymentUseCase(cardDetails, amount)

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrNull()
        assertTrue(paymentResult is PaymentResult.Pending)
        assertEquals(redirectUrl, (paymentResult as PaymentResult.Pending).redirectUrl)
        assertEquals(paymentId, (paymentResult as PaymentResult.Pending).paymentId)
    }

    @Test
    fun `invoke should return failure result when payment fails`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val amount = 6540
        val errorMessage = "Insufficient funds"
        val expectedResult = PaymentResult.Failure(errorMessage)
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, amount)).thenReturn(Result.success(expectedResult))

        val result = processPaymentUseCase(cardDetails, amount)

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrNull()
        assertTrue(paymentResult is PaymentResult.Failure)
        assertEquals(errorMessage, (paymentResult as PaymentResult.Failure).message)
    }

    @Test
    fun `invoke should return failure when tokenization fails`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val amount = 6540
        val error = Exception("Tokenization failed")
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.failure(error))

        val result = processPaymentUseCase(cardDetails, amount)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        verify(paymentRepository).tokenizeCard(cardDetails)
        verify(paymentRepository, never()).processPayment(anyString(), anyInt())
    }

    @Test
    fun `invoke should return failure when payment processing fails`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val amount = 6540
        val error = Exception("Network error")
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, amount)).thenReturn(Result.failure(error))

        val result = processPaymentUseCase(cardDetails, amount)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        verify(paymentRepository).tokenizeCard(cardDetails)
        verify(paymentRepository).processPayment(token, amount)
    }

    @Test
    fun `invoke should use default amount when not provided`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val defaultAmount = 6540
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, defaultAmount)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        processPaymentUseCase(cardDetails)

        verify(paymentRepository).processPayment(token, defaultAmount)
    }

    @Test
    fun `invoke should use custom amount when provided`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val token = "tok_test123456"
        val customAmount = 10000
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(token))
        `when`(paymentRepository.processPayment(token, customAmount)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        processPaymentUseCase(cardDetails, customAmount)

        verify(paymentRepository).processPayment(token, customAmount)
    }

    @Test
    fun `invoke should not process payment if tokenization fails`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val amount = 6540
        val error = Exception("Tokenization failed")
        
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.failure(error))

        processPaymentUseCase(cardDetails, amount)

        verify(paymentRepository).tokenizeCard(cardDetails)
        verify(paymentRepository, never()).processPayment(anyString(), anyInt())
    }
}

