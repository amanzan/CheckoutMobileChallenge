# Checkout.com Mobile Challenge - 3D Secure Payment Implementation

*Last updated: 2025-11-07*

## 📱 Overview

This Android application implements a complete 3D Secure (3DS) payment flow using Jetpack Compose, following Clean Architecture principles. The app provides a seamless user experience for entering card details, tokenizing the card securely, processing payments with 3DS authentication, and handling verification results with detailed error feedback and graceful network error recovery.

## 🏗️ Architecture

The project follows **Clean Architecture** with clear separation of concerns across three layers:

- **Domain Layer**: Business logic, domain models, repository interfaces, and use cases
- **Data Layer**: Repository implementations, API services, and DTOs
- **Presentation Layer**: UI screens (Compose), ViewModels, and navigation

### Key Architectural Decisions

- **Use Case Pattern**: Business logic is encapsulated in use cases (`TokenizeCardUseCase`, `ProcessPaymentUseCase`, `GetPaymentDetailsUseCase`), ensuring testability and separation of concerns
- **Repository Pattern**: Interface-based abstraction allows for easy testing and future data source changes
- **Dependency Injection**: Hilt is used throughout, with separate Retrofit instances for tokenization (public key) and payments (private key)
- **Unidirectional Data Flow**: ViewModels expose `StateFlow` for UI state, ensuring predictable state management

## 🔄 Payment Flow

1. **Card Input**: User enters card number, expiry date, and CVV with real-time validation
2. **Tokenization**: Card details are securely sent to Checkout.com API to obtain a token
3. **Payment Request**: Token is used to create a payment with 3DS enabled
4. **3DS Verification**: WebView displays the 3DS challenge page
5. **Result Handling**: Success or failure screen with detailed error messages (including error codes)

## 📁 Project Structure

```
app/src/main/java/com/checkout/mobilechallenge/albertomanzano/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── CheckoutApi.kt              # Retrofit API interface
│   │   └── dto/
│   │       ├── TokenRequestDto.kt          # Tokenization request/response DTOs
│   │       ├── TokenResponseDto.kt
│   │       ├── PaymentRequestDto.kt        # Payment request/response DTOs
│   │       ├── PaymentResponseDto.kt
│   │       └── PaymentActionDto.kt         # Payment action DTO for error details
│   └── repository/
│       └── PaymentRepositoryImpl.kt        # Repository implementation
├── domain/
│   ├── model/
│   │   ├── CardDetails.kt                  # Domain model for card details
│   │   └── PaymentResult.kt                # Sealed class for payment outcomes
│   ├── repository/
│   │   └── PaymentRepository.kt            # Repository interface
│   └── usecase/
│       ├── TokenizeCardUseCase.kt          # Card tokenization use case
│       ├── ProcessPaymentUseCase.kt        # Payment processing use case
│       └── GetPaymentDetailsUseCase.kt     # Payment details retrieval use case
├── di/
│   ├── NetworkModule.kt                    # Hilt module for API instances
│   └── RepositoryModule.kt                 # Hilt module for repository bindings
├── ui/
│   ├── payment/
│   │   ├── CardInputScreen.kt              # Card input UI with validation
│   │   ├── PaymentViewModel.kt             # ViewModel for payment flow
│   │   ├── ThreeDSWebViewScreen.kt         # WebView for 3DS verification
│   │   └── PaymentResultScreen.kt          # Success/failure result screens
│   └── Navigation.kt                       # Navigation setup
└── utils/
    └── CardUtils.kt                        # Card validation and formatting utilities
```

## ✨ Key Features

### Card Input & Validation
- Real-time card number formatting (Visa/Mastercard: 4-4-4-4, Amex: 4-6-5)
- Automatic card type detection with text badge showing card type (Visa, Mastercard, Amex)
- Luhn algorithm validation for card numbers
- Expiry date validation (MM/YY format, future dates only)
- CVV validation (3 digits for Visa/MC, 4 for Amex)
- Visual feedback for invalid inputs with error messages

### Payment Processing
- Secure tokenization of card details via Checkout.com API
- Payment request with 3DS enabled
- Automatic redirect to 3DS challenge page
- WebView integration for 3DS verification
- Success/failure detection from redirect URLs
- **Detailed error handling**: Extracts specific error codes and messages from API responses and payment actions (e.g., "20051: Insufficient Funds")
- **Network error recovery**: Dedicated error screen for connectivity issues with one-tap retry functionality

### User Experience
- Material Design 3 components and theming
- Loading states during API calls
- Comprehensive error handling with user-friendly messages
- **Graceful network error handling**: Full-screen error UI when no internet connection is detected
- **Smart retry mechanism**: Automatically retries payment with stored card details (no re-entry needed)
- Smooth navigation flow with proper back stack management
- Clear success/failure feedback
- **Accessibility**: Content descriptions and semantic labels for screen readers

## 🔧 Technical Implementation

### Dependency Injection
- **Hilt** for dependency injection
- Separate Retrofit instances for tokenization (public key) and payments (private key)
- Repository pattern with `@Binds` for interface-to-implementation bindings
- Proper scoping with `@Singleton` where appropriate

### State Management
- **ViewModel** with `StateFlow` for reactive UI state
- Unidirectional data flow pattern
- Proper error handling and loading states
- State hoisting for composable functions

### Network Layer
- **Retrofit** for type-safe API calls
- **OkHttp** interceptors for authentication headers
- **Gson** for JSON serialization/deserialization
- Error response parsing with fallback strategies

### WebView Integration
- JavaScript enabled for 3DS challenge pages
- URL monitoring for success/failure detection
- Proper lifecycle handling and state management

## 🧪 Testing

### Unit Tests (5 test files, 79+ test cases)
- **CardUtilsTest**: 42 test cases covering all validation logic (Luhn algorithm, card type detection, expiry/CVV validation, formatting)
- **PaymentViewModelTest**: 20 test cases covering payment flow, error handling, network error detection, retry mechanism, and state management
- **Use Case Tests**: 17 test cases covering all three use cases with proper mocking

### UI Tests (2 test files, 18 test cases)
- **CardInputScreenTest**: 16 test cases covering UI validation, formatting, card type detection, user interactions, and network error UI structure
- **PaymentFlowTest**: 2 integration test cases covering the complete payment flow

All tests use proper coroutine testing with `TestDispatcher`, Mockito for mocking, and Hilt test runner for instrumentation tests.

## 🔑 API Configuration

The app uses Checkout.com sandbox environment:

- **Public Key (Tokenization)**: `pk_sbox_gnrjo6pl5azfmgdnrfrbbejo7ev`
- **Private Key (Payments)**: `sk_sbox_bvzfhwhivsgi33smfjjeb6t64i4`
- **Base URL**: `https://api.sandbox.checkout.com/`

Authentication is handled via `Authorization` header in OkHttp interceptors.

### Security Note

**Sandbox Keys in Repository**: The sandbox API keys are included in this repository for convenience, allowing reviewers to clone and run the project immediately without additional setup. GitHub's secret scanning detected these keys, and I've explicitly allowed them via GitHub's secret scanning unblock feature since these are test/sandbox keys with no production value.

**⚠️ This approach is acceptable ONLY for sandbox/test keys. For production applications, API keys must NEVER be committed to version control.**

### Production Key Management

In a production environment, API keys would be handled as follows:

1. **Android Keystore**: Store keys in Android's secure hardware-backed keystore
2. **Build Config Fields**: Use `BuildConfig` with keys injected at build time (not committed to git)
3. **Environment Variables**: Inject keys via CI/CD pipeline environment variables
4. **Remote Config**: Use Firebase Remote Config or similar services for key distribution
5. **Backend Proxy**: Route API calls through your own backend, keeping keys server-side only

Example production approach:
```kotlin
// Keys would come from secure sources, never hardcoded
private val apiKey = BuildConfig.CHECKOUT_API_KEY // Injected at build time
// OR
private val apiKey = SecurePreferences.getApiKey() // From Android Keystore
// OR
private val apiKey = RemoteConfig.getString("checkout_api_key") // From remote config
```

The keys would be excluded from version control via `.gitignore` and managed through secure deployment pipelines.

## 🧪 Test Cards

### Successful Payment
- **Card Number**: 4242-4242-4242-4242
- **Expiry**: 06/2030
- **CVV**: 100

### Failed Payment (Insufficient Funds)
- **Card Number**: 4544-2491-6767-3670
- **Expiry**: 06/2030
- **CVV**: 100
- **Expected Error**: "20051: Insufficient Funds"

## 📝 Design Decisions & Assumptions

1. **Fixed Payment Amount**: £65.40 (6540 pence) as per challenge requirements
2. **Currency**: GBP as specified
3. **Success/Failure URLs**: Using `example.com/payments/success` and `example.com/payments/fail` as specified
4. **Card Schemes**: Focused on Visa, Mastercard, and Amex (most common schemes)
5. **Error Handling**: Detailed error messages extracted from API responses, including error codes when available. Falls back to payment actions endpoint for more specific error information when needed.
6. **Network Error Recovery**: Separate UI for network connectivity issues (IOException, UnknownHostException) with automatic retry using stored payment details, preventing user frustration from re-entering card information.

## 🚀 Future Enhancements

While the current implementation covers all requirements, potential enhancements for production include:

- **Security**: Move API keys to secure storage (Android Keystore), implement certificate pinning
- **Card Validation**: Support for additional card schemes (Discover, JCB), BIN lookup
- **User Experience**: Payment amount input, saved cards support, card scanning via camera
- **Network Resilience**: Exponential backoff for retries, offline mode detection with connectivity listener
- **Performance**: Request caching, WebView optimization
- **Localization**: Multi-language support, locale-based formatting
- **Analytics**: Payment flow tracking, error monitoring

## 📚 Dependencies

- **Jetpack Compose** - Modern declarative UI toolkit
- **Hilt** - Dependency injection framework
- **Retrofit & OkHttp** - Type-safe HTTP client and networking
- **Gson** - JSON serialization
- **Navigation Compose** - Type-safe navigation
- **Material Design 3** - Material theming and components
- **Coroutines & Flow** - Asynchronous programming
- **Mockito** - Testing framework for mocking

## 🔒 Security Considerations

- **API Keys**: Sandbox keys are included in the repository for convenience (see [Security Note](#security-note) above). Production keys would be stored securely using Android Keystore, BuildConfig, or environment variables
- **Card Data**: Card details are never stored locally - only sent to Checkout.com API via secure HTTPS
- **Network Security**: HTTPS is enforced for all API calls
- **WebView Security**: WebView is configured securely with JavaScript enabled only for 3DS verification
- **No Local Storage**: No sensitive payment data is persisted on the device

## 🏃 Running the Project

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on an emulator or device (API 21+)
5. Use the test cards above to test the payment flow

## 🧪 Running Tests

### Unit Tests
```bash
./gradlew :app:testDebugUnitTest
```

### UI Tests (requires emulator/device)
```bash
./gradlew :app:connectedDebugAndroidTest
```

---

*This project demonstrates senior-level Android development practices including Clean Architecture, comprehensive testing, proper error handling, and attention to user experience and accessibility.*
