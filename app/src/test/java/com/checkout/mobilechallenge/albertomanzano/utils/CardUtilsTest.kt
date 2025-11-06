package com.checkout.mobilechallenge.albertomanzano.utils

import org.junit.Assert.*
import org.junit.Test

class CardUtilsTest {

    @Test
    fun `detectCardType should return VISA for cards starting with 4`() {
        assertEquals(CardUtils.CardType.VISA, CardUtils.detectCardType("4242424242424242"))
        assertEquals(CardUtils.CardType.VISA, CardUtils.detectCardType("4111111111111111"))
        // Single "4" doesn't meet the length requirement (>= 13), so it should be UNKNOWN
        assertEquals(CardUtils.CardType.UNKNOWN, CardUtils.detectCardType("4"))
    }

    @Test
    fun `detectCardType should return MASTERCARD for cards starting with 5`() {
        assertEquals(CardUtils.CardType.MASTERCARD, CardUtils.detectCardType("5555555555554444"))
        assertEquals(CardUtils.CardType.MASTERCARD, CardUtils.detectCardType("5105105105105100"))
    }

    @Test
    fun `detectCardType should return AMEX for cards starting with 34 or 37`() {
        assertEquals(CardUtils.CardType.AMEX, CardUtils.detectCardType("378282246310005"))
        assertEquals(CardUtils.CardType.AMEX, CardUtils.detectCardType("371449635398431"))
        assertEquals(CardUtils.CardType.AMEX, CardUtils.detectCardType("34"))
        assertEquals(CardUtils.CardType.AMEX, CardUtils.detectCardType("37"))
    }

    @Test
    fun `detectCardType should return UNKNOWN for unrecognized cards`() {
        assertEquals(CardUtils.CardType.UNKNOWN, CardUtils.detectCardType("1234567890123456"))
        assertEquals(CardUtils.CardType.UNKNOWN, CardUtils.detectCardType(""))
        assertEquals(CardUtils.CardType.UNKNOWN, CardUtils.detectCardType("6011111111111117")) // Discover
    }

    @Test
    fun `detectCardType should handle formatted card numbers`() {
        assertEquals(CardUtils.CardType.VISA, CardUtils.detectCardType("4242 4242 4242 4242"))
        assertEquals(CardUtils.CardType.MASTERCARD, CardUtils.detectCardType("5555-5555-5555-4444"))
        assertEquals(CardUtils.CardType.AMEX, CardUtils.detectCardType("3782-822463-10005"))
    }

    @Test
    fun `formatCardNumber should format Visa cards in 4-4-4-4 format`() {
        assertEquals("4242 4242 4242 4242", CardUtils.formatCardNumber("4242424242424242"))
        assertEquals("4111 1111 1111 1111", CardUtils.formatCardNumber("4111111111111111"))
        assertEquals("4242", CardUtils.formatCardNumber("4242"))
        assertEquals("4242 4242", CardUtils.formatCardNumber("42424242"))
    }

    @Test
    fun `formatCardNumber should format Mastercard cards in 4-4-4-4 format`() {
        assertEquals("5555 5555 5555 4444", CardUtils.formatCardNumber("5555555555554444"))
        assertEquals("5105 1051 0510 5100", CardUtils.formatCardNumber("5105105105105100"))
    }

    @Test
    fun `formatCardNumber should format Amex cards in 4-6-5 format`() {
        assertEquals("3782-822463-10005", CardUtils.formatCardNumber("378282246310005"))
        assertEquals("3714-496353-98431", CardUtils.formatCardNumber("371449635398431"))
        assertEquals("3782", CardUtils.formatCardNumber("3782"))
        assertEquals("3782-822463", CardUtils.formatCardNumber("3782822463"))
    }

    @Test
    fun `formatCardNumber should return empty string for empty input`() {
        assertEquals("", CardUtils.formatCardNumber(""))
    }

    @Test
    fun `formatCardNumber should remove non-digit characters`() {
        assertEquals("4242 4242 4242 4242", CardUtils.formatCardNumber("4242-4242-4242-4242"))
        assertEquals("4242 4242 4242 4242", CardUtils.formatCardNumber("4242 4242 4242 4242"))
    }

    @Test
    fun `formatExpiryDate should format as MM slash YY`() {
        assertEquals("12/30", CardUtils.formatExpiryDate("1230"))
        assertEquals("06/30", CardUtils.formatExpiryDate("0630"))
        assertEquals("01/25", CardUtils.formatExpiryDate("0125"))
    }

    @Test
    fun `formatExpiryDate should handle partial input`() {
        assertEquals("12", CardUtils.formatExpiryDate("12"))
        assertEquals("1", CardUtils.formatExpiryDate("1"))
        assertEquals("", CardUtils.formatExpiryDate(""))
    }

    @Test
    fun `formatExpiryDate should remove existing slashes`() {
        assertEquals("12/30", CardUtils.formatExpiryDate("12/30"))
        assertEquals("06/30", CardUtils.formatExpiryDate("06/30"))
    }

    @Test
    fun `isValidCardNumber should return true for valid Visa card`() {
        assertTrue(CardUtils.isValidCardNumber("4242424242424242"))
        assertTrue(CardUtils.isValidCardNumber("4111111111111111"))
    }

    @Test
    fun `isValidCardNumber should return true for valid Mastercard`() {
        assertTrue(CardUtils.isValidCardNumber("5555555555554444"))
        assertTrue(CardUtils.isValidCardNumber("5105105105105100"))
    }

    @Test
    fun `isValidCardNumber should return true for valid Amex card`() {
        assertTrue(CardUtils.isValidCardNumber("378282246310005"))
        assertTrue(CardUtils.isValidCardNumber("371449635398431"))
    }

    @Test
    fun `isValidCardNumber should return false for invalid Luhn checksum`() {
        assertFalse(CardUtils.isValidCardNumber("4242424242424241"))
        assertFalse(CardUtils.isValidCardNumber("1234567890123456"))
    }

    @Test
    fun `isValidCardNumber should return false for wrong length`() {
        assertFalse(CardUtils.isValidCardNumber("424242424242424")) // 15 digits for Visa
        assertFalse(CardUtils.isValidCardNumber("42424242424242422")) // 17 digits for Visa
        assertFalse(CardUtils.isValidCardNumber("37828224631000")) // 14 digits for Amex
        assertFalse(CardUtils.isValidCardNumber("3782822463100055")) // 16 digits for Amex
    }

    @Test
    fun `isValidCardNumber should return false for non-digit characters`() {
        // The implementation accepts formatted numbers (with spaces/dashes) and cleans them
        // So formatted valid numbers should still be valid
        // Test with actual invalid characters
        assertFalse(CardUtils.isValidCardNumber("4242a424242424242"))
        assertFalse(CardUtils.isValidCardNumber("4242-4242-4242-424x"))
        assertFalse(CardUtils.isValidCardNumber("4242 4242 4242 424a"))
    }

    @Test
    fun `isValidCardNumber should return false for unknown card type`() {
        assertFalse(CardUtils.isValidCardNumber("1234567890123456"))
    }

    @Test
    fun `isValidExpiryDate should return true for future dates`() {
        // This test assumes current date is before 2030
        assertTrue(CardUtils.isValidExpiryDate("1230"))
        assertTrue(CardUtils.isValidExpiryDate("0630"))
    }

    @Test
    fun `isValidExpiryDate should return false for past dates`() {
        // Get current date to calculate past dates
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR) % 100
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        
        // Test with a date clearly in the past (year 00 = 2000)
        assertFalse(CardUtils.isValidExpiryDate("0100"))
        
        // Test with a date in the past relative to current date
        // Use previous month of previous year
        val pastYear = if (currentMonth == 1) currentYear - 1 else currentYear
        val pastMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val pastDate = String.format("%02d%02d", pastMonth, pastYear)
        assertFalse(CardUtils.isValidExpiryDate(pastDate))
    }

    @Test
    fun `isValidExpiryDate should return false for invalid month`() {
        assertFalse(CardUtils.isValidExpiryDate("0030"))
        assertFalse(CardUtils.isValidExpiryDate("1330"))
        assertFalse(CardUtils.isValidExpiryDate("2530"))
    }

    @Test
    fun `isValidExpiryDate should return false for wrong length`() {
        assertFalse(CardUtils.isValidExpiryDate("123"))
        assertFalse(CardUtils.isValidExpiryDate("12345"))
        assertFalse(CardUtils.isValidExpiryDate(""))
    }

    @Test
    fun `isValidCvv should return true for valid 3-digit CVV`() {
        assertTrue(CardUtils.isValidCvv("123", CardUtils.CardType.VISA))
        assertTrue(CardUtils.isValidCvv("456", CardUtils.CardType.MASTERCARD))
        assertTrue(CardUtils.isValidCvv("789", CardUtils.CardType.UNKNOWN))
    }

    @Test
    fun `isValidCvv should return true for valid 4-digit CVV for Amex`() {
        assertTrue(CardUtils.isValidCvv("1234", CardUtils.CardType.AMEX))
        assertTrue(CardUtils.isValidCvv("5678", CardUtils.CardType.AMEX))
    }

    @Test
    fun `isValidCvv should return false for wrong length`() {
        assertFalse(CardUtils.isValidCvv("12", CardUtils.CardType.VISA))
        assertFalse(CardUtils.isValidCvv("1234", CardUtils.CardType.VISA))
        assertFalse(CardUtils.isValidCvv("123", CardUtils.CardType.AMEX))
        assertFalse(CardUtils.isValidCvv("12345", CardUtils.CardType.AMEX))
    }

    @Test
    fun `isValidCvv should return false for non-digit characters`() {
        assertFalse(CardUtils.isValidCvv("12a", CardUtils.CardType.VISA))
        assertFalse(CardUtils.isValidCvv("abc", CardUtils.CardType.VISA))
    }
}

