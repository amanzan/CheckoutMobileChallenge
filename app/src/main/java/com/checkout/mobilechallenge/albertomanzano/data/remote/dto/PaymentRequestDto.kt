package com.checkout.mobilechallenge.albertomanzano.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PaymentRequestDto(
    val source: PaymentSourceDto,
    val amount: Int,
    val currency: String = "GBP",
    @SerializedName("3ds")
    val threeDs: ThreeDsDto,
    @SerializedName("success_url")
    val successUrl: String,
    @SerializedName("failure_url")
    val failureUrl: String
)

data class PaymentSourceDto(
    val type: String = "token",
    val token: String
)

data class ThreeDsDto(
    val enabled: Boolean = true
)

