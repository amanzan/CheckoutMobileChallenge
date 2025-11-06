package com.checkout.mobilechallenge.albertomanzano.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRequestDto(
    val type: String = "card",
    val number: String,
    @SerializedName("expiry_month")
    val expiryMonth: String,
    @SerializedName("expiry_year")
    val expiryYear: String,
    val cvv: String
)

