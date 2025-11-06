package com.checkout.mobilechallenge.albertomanzano.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PaymentActionDto(
    val id: String? = null,
    val type: String? = null,
    val status: String? = null,
    @SerializedName("response_summary")
    val responseSummary: String? = null,
    @SerializedName("response_code")
    val responseCode: String? = null,
    @SerializedName("decline_reason")
    val declineReason: String? = null,
    val message: String? = null
)

