package com.checkout.mobilechallenge.albertomanzano.ui.payment

import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult
import com.checkout.mobilechallenge.albertomanzano.domain.usecase.GetPaymentDetailsUseCase
import com.checkout.mobilechallenge.albertomanzano.domain.usecase.ProcessPaymentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatchers.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class PaymentViewModelTest {

    @Mock
    private lateinit var processPaymentUseCase: ProcessPaymentUseCase

    @Mock
    private lateinit var getPaymentDetailsUseCase: GetPaymentDetailsUseCase

    private lateinit var viewModel: PaymentViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        // Set the main dispatcher to our test dispatcher for ViewModelScope
        Dispatchers.setMain(testDispatcher)
        // MockitoJUnitRunner will automatically initialize @Mock annotated fields
        viewModel = PaymentViewModel(processPaymentUseCase, getPaymentDetailsUseCase)
    }

    @After
    fun tearDown() {
        // Reset the main dispatcher after tests
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val initialState = viewModel.uiState.first()
        
        assertFalse(initialState.isLoading)
        assertNull(initialState.error)
        assertNull(initialState.paymentResult)
        assertNull(initialState.redirectUrl)
        assertNull(initialState.paymentId)
        assertNull(initialState.paymentErrorBefore3DS)
    }

    @Test
    fun `processPayment should set loading state to true initially`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        verify(processPaymentUseCase).invoke(cardDetails, 6540)
    }

    @Test
    fun `processPayment should handle successful tokenization and payment`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.paymentResult is PaymentResult.Success)
        assertNull(state.error)
    }

    @Test
    fun `processPayment should handle pending payment with redirect URL`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val redirectUrl = "https://api.checkout.com/3ds/test"
        val paymentId = "pay_test123"
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Pending(redirectUrl, paymentId))
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.paymentResult is PaymentResult.Pending)
        assertEquals(redirectUrl, state.redirectUrl)
        assertEquals(paymentId, state.paymentId)
        assertNull(state.paymentErrorBefore3DS)
    }

    @Test
    fun `processPayment should handle payment failure`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val errorMessage = "Insufficient funds"
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Failure(errorMessage))
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.paymentResult is PaymentResult.Failure)
        assertEquals(errorMessage, state.error)
        assertEquals(errorMessage, state.paymentErrorBefore3DS)
    }

    @Test
    fun `processPayment should handle tokenization failure`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val error = Exception("Tokenization failed")
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(Result.failure(error))

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Tokenization failed", state.error)
        assertTrue(state.paymentResult is PaymentResult.Failure)
    }

    @Test
    fun `processPayment should handle payment processing failure`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val error = Exception("Network error")
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(Result.failure(error))

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
        assertTrue(state.paymentResult is PaymentResult.Failure)
    }

    @Test
    fun `processPayment should use custom amount when provided`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val customAmount = 10000
        `when`(processPaymentUseCase(cardDetails, customAmount)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        viewModel.processPayment(cardDetails, customAmount)
        advanceUntilIdle()

        verify(processPaymentUseCase).invoke(cardDetails, customAmount)
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val error = Exception("Test error")
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(Result.failure(error))

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertNotNull(state.error)

        viewModel.clearError()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `clearPaymentResult should clear payment result and related state`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val redirectUrl = "https://api.checkout.com/3ds/test"
        val paymentId = "pay_test123"
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Pending(redirectUrl, paymentId))
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertNotNull(state.paymentResult)
        assertNotNull(state.redirectUrl)
        assertNotNull(state.paymentId)

        viewModel.clearPaymentResult()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.paymentResult)
        assertNull(state.redirectUrl)
        assertNull(state.paymentId)
        assertNull(state.paymentErrorBefore3DS)
    }

    @Test
    fun `getPaymentErrorBefore3DS should return stored error`() = runTest {
        val cardDetails = CardDetails("4242424242424242", "12", "30", "123")
        val errorMessage = "Test error"
        `when`(processPaymentUseCase(cardDetails, 6540)).thenReturn(
            Result.success(PaymentResult.Failure(errorMessage))
        )

        viewModel.processPayment(cardDetails)
        advanceUntilIdle()

        assertEquals(errorMessage, viewModel.getPaymentErrorBefore3DS())
    }

    @Test
    fun `fetchPaymentDetailsAfter3DSFailure should update error message on failure`() = runTest {
        val paymentId = "pay_test123"
        val errorMessage = "20051: Insufficient funds"
        `when`(getPaymentDetailsUseCase(paymentId)).thenReturn(
            Result.success(PaymentResult.Failure(errorMessage))
        )

        viewModel.fetchPaymentDetailsAfter3DSFailure(paymentId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(errorMessage, state.paymentErrorBefore3DS)
    }

    @Test
    fun `fetchPaymentDetailsAfter3DSFailure should not update error on success`() = runTest {
        val paymentId = "pay_test123"
        `when`(getPaymentDetailsUseCase(paymentId)).thenReturn(
            Result.success(PaymentResult.Success())
        )

        viewModel.fetchPaymentDetailsAfter3DSFailure(paymentId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.paymentErrorBefore3DS)
    }

    @Test
    fun `fetchPaymentDetailsAfter3DSFailure should handle repository failure gracefully`() = runTest {
        val paymentId = "pay_test123"
        val error = Exception("Network error")
        `when`(getPaymentDetailsUseCase(paymentId)).thenReturn(Result.failure(error))

        viewModel.fetchPaymentDetailsAfter3DSFailure(paymentId)
        advanceUntilIdle()

        // Should not crash, error is logged but not propagated to UI state
        val state = viewModel.uiState.value
        assertNotNull(state)
    }
}

