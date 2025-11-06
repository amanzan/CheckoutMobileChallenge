package com.checkout.mobilechallenge.albertomanzano.utils

/**
 * Utility object for credit card validation and formatting.
 * Provides functions for card type detection, number formatting, and validation.
 */
object CardUtils {
    
    /**
     * Enum representing different card types supported by the application.
     */
    enum class CardType {
        VISA, MASTERCARD, AMEX, UNKNOWN
    }
    
    /**
     * Detects the card type based on the card number.
     * 
     * @param cardNumber The card number (can include spaces or dashes)
     * @return The detected card type, or UNKNOWN if not recognized
     */
    fun detectCardType(cardNumber: String): CardType {
        val cleaned = cardNumber.replace(" ", "").replace("-", "")
        return when {
            cleaned.startsWith("4") && cleaned.length >= 13 -> CardType.VISA
            cleaned.startsWith("5") && cleaned.length == 16 -> CardType.MASTERCARD
            cleaned.startsWith("34") || cleaned.startsWith("37") -> CardType.AMEX
            else -> CardType.UNKNOWN
        }
    }
    
    /**
     * Formats a card number with appropriate spacing based on card type.
     * Visa/Mastercard: 4-4-4-4 format (e.g., 4242 4242 4242 4242)
     * Amex: 4-6-5 format (e.g., 3782-822463-10005)
     *
     * @param cardNumber The raw card number (digits only or with formatting)
     * @return Formatted card number with appropriate spacing
     */
    fun formatCardNumber(cardNumber: String): String {
        // Remove all non-digit characters
        val cleaned = cardNumber.filter { it.isDigit() }
        
        // Don't format if empty
        if (cleaned.isEmpty()) return ""
        
        val cardType = detectCardType(cleaned)
        
        return when (cardType) {
            CardType.AMEX -> {
                // Amex: 4-6-5 format (e.g., 3782-822463-10005)
                when {
                    cleaned.length <= 4 -> cleaned
                    cleaned.length <= 10 -> "${cleaned.substring(0, 4)}-${cleaned.substring(4)}"
                    else -> "${cleaned.substring(0, 4)}-${cleaned.substring(4, 10)}-${cleaned.substring(10)}"
                }
            }
            else -> {
                // Visa/Mastercard: 4-4-4-4 format
                // Process digits sequentially in groups of 4
                buildString {
                    var i = 0
                    while (i < cleaned.length) {
                        if (i > 0 && i % 4 == 0) {
                            append(" ")
                        }
                        append(cleaned[i])
                        i++
                    }
                }
            }
        }
    }
    
    /**
     * Formats an expiry date string to MM/YY format.
     *
     * @param input The expiry date input (can include or exclude the slash)
     * @return Formatted expiry date in MM/YY format
     */
    fun formatExpiryDate(input: String): String {
        val cleaned = input.replace("/", "").replace(" ", "")
        return when {
            cleaned.length <= 2 -> cleaned
            cleaned.length > 2 -> "${cleaned.substring(0, 2)}/${cleaned.substring(2, minOf(4, cleaned.length))}"
            else -> cleaned
        }
    }
    
    /**
     * Validates a card number by checking:
     * - Card type detection
     * - Correct length for the card type
     * - All digits (after removing formatting)
     * - Luhn algorithm checksum
     *
     * @param cardNumber The card number to validate (can include spaces or dashes)
     * @return true if the card number is valid, false otherwise
     */
    fun isValidCardNumber(cardNumber: String): Boolean {
        val cleaned = cardNumber.replace(" ", "").replace("-", "")
        
        // Check if there are any non-digit characters (after removing spaces and dashes)
        if (!cleaned.all { it.isDigit() }) return false
        
        val cardType = detectCardType(cleaned)
        
        val expectedLength = when (cardType) {
            CardType.AMEX -> 15
            CardType.VISA, CardType.MASTERCARD -> 16
            CardType.UNKNOWN -> return false
        }
        
        return cleaned.length == expectedLength && luhnCheck(cleaned)
    }
    
    /**
     * Validates an expiry date by checking:
     * - Format is MM/YY (4 digits)
     * - Month is between 01-12
     * - Date is not in the past
     *
     * @param expiryDate The expiry date in MM/YY format (slash optional)
     * @return true if the expiry date is valid and not expired, false otherwise
     */
    fun isValidExpiryDate(expiryDate: String): Boolean {
        val cleaned = expiryDate.replace("/", "").replace(" ", "")
        if (cleaned.length != 4) return false
        
        val month = cleaned.substring(0, 2).toIntOrNull() ?: return false
        val year = cleaned.substring(2, 4).toIntOrNull() ?: return false
        
        if (month < 1 || month > 12) return false
        
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        
        return year > currentYear || (year == currentYear && month >= currentMonth)
    }
    
    /**
     * Validates a CVV code based on card type.
     * Visa/Mastercard: 3 digits
     * Amex: 4 digits
     *
     * @param cvv The CVV code to validate
     * @param cardType The card type to determine expected length
     * @return true if the CVV is valid for the given card type, false otherwise
     */
    fun isValidCvv(cvv: String, cardType: CardType): Boolean {
        val cleaned = cvv.replace(" ", "")
        val expectedLength = when (cardType) {
            CardType.AMEX -> 4
            else -> 3
        }
        return cleaned.length == expectedLength && cleaned.all { it.isDigit() }
    }
    
    private fun luhnCheck(cardNumber: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}

