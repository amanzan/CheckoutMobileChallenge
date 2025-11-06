package com.checkout.mobilechallenge.albertomanzano.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.checkout.mobilechallenge.albertomanzano.domain.model.CardDetails
import com.checkout.mobilechallenge.albertomanzano.utils.CardUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardInputScreen(
    onPaymentInitiated: (String, String?, String?) -> Unit, // redirectUrl, errorMessage, paymentId
    onPaymentFailed: (String?) -> Unit = {},
    viewModel: PaymentViewModel = hiltViewModel()
) {
    // Store raw digits only, no formatting
    var cardNumberRaw by remember { mutableStateOf("") }
    var expiryDateRaw by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    
    val uiState by viewModel.uiState.collectAsState()
    val cardType = CardUtils.detectCardType(cardNumberRaw)
    
    // Visual transformation for card number formatting
    val cardNumberVisualTransformation = remember(cardType) {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val formatted = CardUtils.formatCardNumber(text.text)
                return TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            // Map original offset to formatted offset
                            val digitsBefore = text.text.substring(0, minOf(offset, text.text.length))
                            return CardUtils.formatCardNumber(digitsBefore).length
                        }
                        
                        override fun transformedToOriginal(offset: Int): Int {
                            // Map formatted offset back to original
                            val formatted = CardUtils.formatCardNumber(text.text)
                            val beforeCursor = formatted.substring(0, minOf(offset, formatted.length))
                            return beforeCursor.filter { it.isDigit() }.length
                        }
                    }
                )
            }
        }
    }
    
    // Visual transformation for expiry date formatting
    val expiryDateVisualTransformation = remember {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val formatted = CardUtils.formatExpiryDate(text.text)
                return TransformedText(
                    AnnotatedString(formatted),
                    object : OffsetMapping {
                        override fun originalToTransformed(offset: Int): Int {
                            // Map original offset to formatted offset
                            val digitsBefore = text.text.substring(0, minOf(offset, text.text.length))
                            return CardUtils.formatExpiryDate(digitsBefore).length
                        }
                        
                        override fun transformedToOriginal(offset: Int): Int {
                            // Map formatted offset back to original
                            val formatted = CardUtils.formatExpiryDate(text.text)
                            val beforeCursor = formatted.substring(0, minOf(offset, formatted.length))
                            return beforeCursor.filter { it.isDigit() }.length
                        }
                    }
                )
            }
        }
    }
    
    // Clear state when screen is shown (but only if not currently processing)
    LaunchedEffect(Unit) {
        if (!uiState.isLoading && uiState.paymentResult == null) {
            viewModel.clearPaymentResult()
            viewModel.clearError()
        }
    }
    
    // Handle successful payment initiation (redirect to 3DS)
    LaunchedEffect(uiState.redirectUrl, uiState.paymentErrorBefore3DS, uiState.paymentId) {
        uiState.redirectUrl?.let { url ->
            // Store payment error and payment ID before navigating to 3DS
            // Payment ID will be used to fetch actual error details after 3DS fails
            val errorToPass = uiState.paymentErrorBefore3DS
            val paymentIdToPass = uiState.paymentId
            onPaymentInitiated(url, errorToPass, paymentIdToPass)
        }
    }
    
    // Handle payment failures that occur before 3DS (no redirect URL)
    LaunchedEffect(uiState.paymentResult, uiState.redirectUrl, uiState.isLoading) {
        // Only navigate to failure screen if:
        // 1. There's a failure result
        // 2. No redirect URL (meaning no 3DS flow)
        // 3. Not currently loading
        if (uiState.redirectUrl == null && !uiState.isLoading) {
            (uiState.paymentResult as? com.checkout.mobilechallenge.albertomanzano.domain.model.PaymentResult.Failure)?.let { failure ->
                onPaymentFailed(failure.message)
                // Clear the result after navigating to prevent re-triggering
                viewModel.clearPaymentResult()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Card Payment",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(bottom = 32.dp)
                .semantics { contentDescription = "Card Payment Screen Title" }
        )
        
        // Card Number Input
        OutlinedTextField(
            value = cardNumberRaw,
            onValueChange = { newValue ->
                // Only allow digits, limit length
                val digitsOnly = newValue.filter { it.isDigit() }
                val maxLength = if (digitsOnly.isNotEmpty() && CardUtils.detectCardType(digitsOnly) == CardUtils.CardType.AMEX) 15 else 16
                cardNumberRaw = digitsOnly.take(maxLength)
            },
            label = { Text("Card Number") },
            placeholder = { Text("1234 5678 9012 3456") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics { 
                    contentDescription = "Card number input field. Enter your ${if (cardType != CardUtils.CardType.UNKNOWN) when (cardType) {
                        CardUtils.CardType.VISA -> "Visa"
                        CardUtils.CardType.MASTERCARD -> "Mastercard"
                        CardUtils.CardType.AMEX -> "American Express"
                        else -> ""
                    } else ""} card number."
                },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = cardNumberVisualTransformation,
            isError = cardNumberRaw.isNotEmpty() && !CardUtils.isValidCardNumber(cardNumberRaw),
            supportingText = {
                if (cardNumberRaw.isNotEmpty() && !CardUtils.isValidCardNumber(cardNumberRaw)) {
                    Text("Invalid card number")
                }
            },
            trailingIcon = {
                if (cardType != CardUtils.CardType.UNKNOWN) {
                    Text(
                        text = when (cardType) {
                            CardUtils.CardType.VISA -> "VISA"
                            CardUtils.CardType.MASTERCARD -> "MC"
                            CardUtils.CardType.AMEX -> "AMEX"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { 
                            contentDescription = "Detected card type: ${when (cardType) {
                                CardUtils.CardType.VISA -> "Visa"
                                CardUtils.CardType.MASTERCARD -> "Mastercard"
                                CardUtils.CardType.AMEX -> "American Express"
                                else -> ""
                            }}"
                        }
                    )
                }
            }
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Expiry Date Input
            OutlinedTextField(
                value = expiryDateRaw,
                onValueChange = { newValue ->
                    // Only allow digits, limit to 4 digits (MMYY)
                    val digitsOnly = newValue.filter { it.isDigit() }
                    expiryDateRaw = digitsOnly.take(4)
                },
                label = { Text("Expiry Date") },
                placeholder = { Text("MM/YY") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { 
                        contentDescription = "Expiry date input field. Enter card expiration date in MM/YY format."
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = expiryDateVisualTransformation,
                isError = expiryDateRaw.isNotEmpty() && !CardUtils.isValidExpiryDate(CardUtils.formatExpiryDate(expiryDateRaw)),
                supportingText = {
                    if (expiryDateRaw.isNotEmpty() && !CardUtils.isValidExpiryDate(CardUtils.formatExpiryDate(expiryDateRaw))) {
                        Text("Invalid date")
                    }
                }
            )
            
            // CVV Input
            OutlinedTextField(
                value = cvv,
                onValueChange = { newValue ->
                    val cleaned = newValue.filter { it.isDigit() }
                    if (cleaned.length <= 4) {
                        cvv = cleaned
                    }
                },
                label = { Text("CVV") },
                placeholder = { Text(if (cardType == CardUtils.CardType.AMEX) "1234" else "123") },
                modifier = Modifier
                    .weight(1f)
                    .semantics { 
                        contentDescription = "CVV input field. Enter the ${if (cardType == CardUtils.CardType.AMEX) "4-digit" else "3-digit"} security code from the back of your card."
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = cvv.isNotEmpty() && !CardUtils.isValidCvv(cvv, cardType),
                supportingText = {
                    if (cvv.isNotEmpty() && !CardUtils.isValidCvv(cvv, cardType)) {
                        Text("Invalid CVV")
                    }
                }
            )
        }
        
        // Error message
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Pay Button
        Button(
            onClick = {
                val month = if (expiryDateRaw.length >= 2) expiryDateRaw.substring(0, 2) else ""
                val year = if (expiryDateRaw.length >= 4) "20${expiryDateRaw.substring(2, 4)}" else ""
                val formattedExpiry = CardUtils.formatExpiryDate(expiryDateRaw)
                
                if (CardUtils.isValidCardNumber(cardNumberRaw) &&
                    CardUtils.isValidExpiryDate(formattedExpiry) &&
                    CardUtils.isValidCvv(cvv, cardType)) {
                    
                    val cardDetails = CardDetails(
                        number = cardNumberRaw,
                        expiryMonth = month,
                        expiryYear = year,
                        cvv = cvv
                    )
                    viewModel.processPayment(cardDetails)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics { 
                    contentDescription = if (uiState.isLoading) {
                        "Processing payment, please wait"
                    } else {
                        "Pay button. Process payment of £65.40"
                    }
                },
            enabled = !uiState.isLoading &&
                    CardUtils.isValidCardNumber(cardNumberRaw) &&
                    CardUtils.isValidExpiryDate(CardUtils.formatExpiryDate(expiryDateRaw)) &&
                    CardUtils.isValidCvv(cvv, cardType)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .semantics { contentDescription = "Loading indicator" },
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Pay £65.40")
            }
        }
    }
}

