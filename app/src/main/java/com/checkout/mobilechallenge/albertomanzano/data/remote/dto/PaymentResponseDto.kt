package com.checkout.mobilechallenge.albertomanzano.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PaymentResponseDto(
    val id: String? = null,
    val status: String? = null,
    @SerializedName("_links")
    val links: PaymentLinksDto? = null,
    @SerializedName("response_summary")
    val responseSummary: String? = null,
    @SerializedName("response_code")
    val responseCode: String? = null,
    @SerializedName("error_type")
    val errorType: String? = null,
    @SerializedName("error_codes")
    val errorCodes: List<String>? = null,
    @SerializedName("decline_reason")
    val declineReason: String? = null,
    val message: String? = null,
    @SerializedName("request_id")
    val requestId: String? = null
)

data class PaymentLinksDto(
    val redirect: RedirectLinkDto?
)

data class RedirectLinkDto(
    val href: String
)

