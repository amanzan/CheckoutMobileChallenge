package com.checkout.mobilechallenge.albertomanzano.domain.usecase

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
class GetPaymentDetailsUseCaseTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    private lateinit var getPaymentDetailsUseCase: GetPaymentDetailsUseCase

    @Before
    fun setup() {
        getPaymentDetailsUseCase = GetPaymentDetailsUseCase(paymentRepository)
    }

    @Test
    fun `invoke should return success when repository returns success result`() = runTest {
        val paymentId = "pay_test123"
        val expectedResult = PaymentResult.Success()
        
        `when`(paymentRepository.getPaymentDetails(paymentId)).thenReturn(Result.success(expectedResult))

        val result = getPaymentDetailsUseCase(paymentId)

        assertTrue(result.isSuccess)
        assertEquals(expectedResult, result.getOrNull())
        verify(paymentRepository).getPaymentDetails(paymentId)
    }

    @Test
    fun `invoke should return failure result when payment failed`() = runTest {
        val paymentId = "pay_test123"
        val errorMessage = "20051: Insufficient funds"
        val expectedResult = PaymentResult.Failure(errorMessage)
        
        `when`(paymentRepository.getPaymentDetails(paymentId)).thenReturn(Result.success(expectedResult))

        val result = getPaymentDetailsUseCase(paymentId)

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrNull()
        assertTrue(paymentResult is PaymentResult.Failure)
        assertEquals(errorMessage, (paymentResult as PaymentResult.Failure).message)
    }

    @Test
    fun `invoke should return pending result when payment is pending`() = runTest {
        val paymentId = "pay_test123"
        val redirectUrl = "https://api.checkout.com/3ds/test"
        val expectedResult = PaymentResult.Pending(redirectUrl, paymentId)
        
        `when`(paymentRepository.getPaymentDetails(paymentId)).thenReturn(Result.success(expectedResult))

        val result = getPaymentDetailsUseCase(paymentId)

        assertTrue(result.isSuccess)
        val paymentResult = result.getOrNull()
        assertTrue(paymentResult is PaymentResult.Pending)
        assertEquals(redirectUrl, (paymentResult as PaymentResult.Pending).redirectUrl)
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        val paymentId = "pay_test123"
        val error = Exception("Network error")
        
        `when`(paymentRepository.getPaymentDetails(paymentId)).thenReturn(Result.failure(error))

        val result = getPaymentDetailsUseCase(paymentId)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        verify(paymentRepository).getPaymentDetails(paymentId)
    }

    @Test
    fun `invoke should delegate to repository with correct payment ID`() = runTest {
        val paymentId = "pay_test123"
        `when`(paymentRepository.getPaymentDetails(paymentId)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        getPaymentDetailsUseCase(paymentId)

        verify(paymentRepository, times(1)).getPaymentDetails(paymentId)
        verifyNoMoreInteractions(paymentRepository)
    }

    @Test
    fun `invoke should handle different payment IDs correctly`() = runTest {
        val paymentId1 = "pay_test123"
        val paymentId2 = "pay_test456"
        
        `when`(paymentRepository.getPaymentDetails(paymentId1)).thenReturn(
            Result.success(PaymentResult.Success())
        )
        `when`(paymentRepository.getPaymentDetails(paymentId2)).thenReturn(
            Result.success(PaymentResult.Failure("Error"))
        )

        val result1 = getPaymentDetailsUseCase(paymentId1)
        val result2 = getPaymentDetailsUseCase(paymentId2)

        assertTrue(result1.isSuccess)
        assertTrue(result1.getOrNull() is PaymentResult.Success)
        assertTrue(result2.isSuccess)
        assertTrue(result2.getOrNull() is PaymentResult.Failure)
        verify(paymentRepository).getPaymentDetails(paymentId1)
        verify(paymentRepository).getPaymentDetails(paymentId2)
    }
}

