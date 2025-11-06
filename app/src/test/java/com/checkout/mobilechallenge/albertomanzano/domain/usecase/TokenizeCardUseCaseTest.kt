package com.checkout.mobilechallenge.albertomanzano.domain.usecase

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
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
class TokenizeCardUseCaseTest {

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    private lateinit var tokenizeCardUseCase: TokenizeCardUseCase

    @Before
    fun setup() {
        tokenizeCardUseCase = TokenizeCardUseCase(paymentRepository)
    }

    @Test
    fun `invoke should return success when repository tokenizes card successfully`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val expectedToken = "tok_test123456"
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success(expectedToken))

        val result = tokenizeCardUseCase(cardDetails)

        assertTrue(result.isSuccess)
        assertEquals(expectedToken, result.getOrNull())
        verify(paymentRepository).tokenizeCard(cardDetails)
    }

    @Test
    fun `invoke should return failure when repository fails to tokenize`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val error = Exception("Tokenization failed")
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.failure(error))

        val result = tokenizeCardUseCase(cardDetails)

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
        verify(paymentRepository).tokenizeCard(cardDetails)
    }

    @Test
    fun `invoke should delegate to repository with correct card details`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        `when`(paymentRepository.tokenizeCard(cardDetails)).thenReturn(Result.success("token"))

        tokenizeCardUseCase(cardDetails)

        verify(paymentRepository, times(1)).tokenizeCard(cardDetails)
        verifyNoMoreInteractions(paymentRepository)
    }
}

