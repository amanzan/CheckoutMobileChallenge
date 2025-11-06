package com.checkout.mobilechallenge.albertomanzano.ui.payment

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.checkout.mobilechallenge.albertomanzano.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the payment flow.
 * Tests the complete user journey through the payment process.
 * 
 * Note: These tests use the real API endpoints. For more reliable tests,
 * consider using a mock server or test doubles.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PaymentFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun paymentFlow_completeFlow() {
        // Wait for MainActivity to load
        composeTestRule.onNodeWithText("Card Payment")
            .assertIsDisplayed()
        
        // Enter valid card details
        composeTestRule.onNodeWithText("Card Number")
            .performTextInput("4242424242424242")
        composeTestRule.onNodeWithText("Expiry Date")
            .performTextInput("1230")
        composeTestRule.onNodeWithText("CVV")
            .performTextInput("123")
        
        // Verify pay button is enabled
        composeTestRule.onNodeWithText("Pay £65.40")
            .assertIsEnabled()
        
        // Note: Clicking pay would trigger actual API calls.
        // For production tests, you'd want to mock the API responses.
    }

    @Test
    fun paymentFlow_validationPreventsInvalidSubmission() {
        composeTestRule.onNodeWithText("Card Payment")
            .assertIsDisplayed()
        
        // Enter invalid card number
        composeTestRule.onNodeWithText("Card Number")
            .performTextInput("1234")
        
        // Pay button should remain disabled
        composeTestRule.onNodeWithText("Pay £65.40")
            .assertIsNotEnabled()
        
        // Error message should be displayed
        composeTestRule.onNodeWithText("Invalid card number")
            .assertIsDisplayed()
    }
}

