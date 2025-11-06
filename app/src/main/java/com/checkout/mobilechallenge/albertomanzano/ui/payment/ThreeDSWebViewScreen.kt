package com.checkout.mobilechallenge.albertomanzano.ui.payment

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDSWebViewScreen(
    redirectUrl: String,
    onSuccess: () -> Unit,
    onFailure: (errorMessage: String?, paymentId: String?) -> Unit, // Now also passes paymentId
    onDismiss: () -> Unit,
    paymentErrorBefore3DS: String? = null, // Error message from payment response before 3DS
    paymentId: String? = null // Payment ID to retrieve details after 3DS fails
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasHandledResult by remember { mutableStateOf(false) }

    fun checkUrl(url: String?) {
        if (hasHandledResult) return
        
        url?.let { currentUrl ->
            when {
                currentUrl.contains("example.com/payments/success") -> {
                    hasHandledResult = true
                    onSuccess()
                }
                currentUrl.contains("example.com/payments/fail") -> {
                    hasHandledResult = true
                    // Pass paymentId so we can fetch actual error details
                    // Use payment error from before 3DS if available, otherwise use generic message
                    val errorMessage = paymentErrorBefore3DS 
                        ?: "3D Secure authentication failed. Please try again."
                    onFailure(errorMessage, paymentId)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = { 
                Text(
                    "3D Secure Verification",
                    modifier = Modifier.semantics { 
                        contentDescription = "3D Secure Verification Screen"
                    }
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.semantics { 
                        contentDescription = "Back button. Return to card input screen"
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Back"
                    )
                }
            }
        )

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            request?.url?.toString()?.let { url ->
                                checkUrl(url)
                            }
                            return false
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            checkUrl(url)
                        }
                    }
                    
                    loadUrl(redirectUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .semantics { 
                        contentDescription = "Loading 3D Secure verification page"
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics { 
                        contentDescription = "Loading indicator"
                    }
                )
            }
        }
    }
}

