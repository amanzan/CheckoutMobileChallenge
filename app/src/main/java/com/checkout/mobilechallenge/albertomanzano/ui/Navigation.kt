package com.checkout.mobilechallenge.albertomanzano.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.checkout.mobilechallenge.albertomanzano.ui.payment.CardInputScreen
import com.checkout.mobilechallenge.albertomanzano.ui.payment.PaymentFailureScreen
import com.checkout.mobilechallenge.albertomanzano.ui.payment.PaymentSuccessScreen
import com.checkout.mobilechallenge.albertomanzano.ui.payment.PaymentViewModel
import com.checkout.mobilechallenge.albertomanzano.ui.payment.ThreeDSWebViewScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "card_input") {
        composable("card_input") {
            CardInputScreen(
                onPaymentInitiated = { redirectUrl, paymentError, paymentId ->
                    val encodedUrl = java.net.URLEncoder.encode(redirectUrl, "UTF-8")
                    val encodedPaymentId = paymentId?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                    // Store payment error in saved state (if any)
                    if (paymentError != null) {
                        navController.currentBackStackEntry?.savedStateHandle?.set("payment_error_before_3ds", paymentError)
                    }
                    // Pass payment ID as navigation argument
                    navController.navigate("three_ds/$encodedUrl/$encodedPaymentId")
                },
                onPaymentFailed = { errorMessage ->
                    val encodedError = errorMessage?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                    navController.navigate("payment_failure/$encodedError") {
                        popUpTo("card_input") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "three_ds/{redirectUrl}/{paymentId}",
            arguments = listOf(
                navArgument("redirectUrl") { type = NavType.StringType },
                navArgument("paymentId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("redirectUrl") ?: ""
            val redirectUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
            val encodedPaymentId = backStackEntry.arguments?.getString("paymentId")
            val paymentId = encodedPaymentId?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            // Get payment error from saved state (if any)
            val paymentErrorBefore3DS = backStackEntry.savedStateHandle.get<String>("payment_error_before_3ds")
            ThreeDSWebViewScreen(
                redirectUrl = redirectUrl,
                onSuccess = {
                    navController.navigate("payment_success") {
                        // Pop everything back to card_input (removes three_ds and any existing result screens)
                        popUpTo("card_input") { inclusive = false }
                        // Prevent duplicate success screens
                        launchSingleTop = true
                    }
                },
                onFailure = { errorMessage, paymentId ->
                    val encodedError = errorMessage?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                    val encodedPaymentId = paymentId?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                    // Pass payment ID as navigation argument
                    navController.navigate("payment_failure/$encodedError/$encodedPaymentId") {
                        // Pop everything back to card_input (removes three_ds and any existing result screens)
                        popUpTo("card_input") { inclusive = false }
                        // Prevent duplicate failure screens
                        launchSingleTop = true
                    }
                },
                onDismiss = {
                    // Cancel 3DS process and return to card input screen
                    navController.navigate("card_input") {
                        popUpTo("card_input") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                paymentErrorBefore3DS = paymentErrorBefore3DS,
                paymentId = paymentId
            )
        }
        composable("payment_success") {
            PaymentSuccessScreen(
                onContinue = {
                    // Navigate back to card_input and clear the entire stack
                    navController.navigate("card_input") {
                        // Pop everything including the current success screen
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = "payment_failure/{errorMessage}/{paymentId}",
            arguments = listOf(
                navArgument("errorMessage") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("paymentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
        )) { backStackEntry ->
            // Get error message from navigation arguments and decode it
            val encodedError = backStackEntry.arguments?.getString("errorMessage")
            val errorMessage = encodedError?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            // Get payment ID from navigation arguments and decode it
            val encodedPaymentId = backStackEntry.arguments?.getString("paymentId")
            val paymentId = encodedPaymentId?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            
            // Fetch payment details if we have a payment ID and the error is generic
            val viewModel: PaymentViewModel = hiltViewModel()
            val shouldFetchDetails = paymentId != null && (errorMessage == null || errorMessage.contains("3D Secure authentication failed"))
            if (shouldFetchDetails) {
                LaunchedEffect(paymentId) {
                    viewModel.fetchPaymentDetailsAfter3DSFailure(paymentId)
                }
            }
            // Observe the updated error message
            val uiState by viewModel.uiState.collectAsState()
            val actualErrorMessage = if (shouldFetchDetails) {
                uiState.paymentErrorBefore3DS ?: errorMessage
            } else {
                errorMessage
            }
            
            PaymentFailureScreen(
                errorMessage = actualErrorMessage,
                onHome = {
                    // Navigate back to card_input and clear the entire stack
                    navController.navigate("card_input") {
                        // Pop everything including the current failure screen
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
