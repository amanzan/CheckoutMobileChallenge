package com.checkout.mobilechallenge.albertomanzano.data.remote.api

import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentActionDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentRequestDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.PaymentResponseDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.TokenRequestDto
import com.checkout.mobilechallenge.albertomanzano.data.remote.dto.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface CheckoutApi {

    @POST("tokens")
    @Headers("Content-Type: application/json")
    suspend fun requestToken(@Body request: TokenRequestDto): TokenResponseDto

    @POST("payments")
    @Headers("Content-Type: application/json")
    suspend fun requestPayment(@Body request: PaymentRequestDto): PaymentResponseDto

    @GET("payments/{paymentId}")
    @Headers("Content-Type: application/json")
    suspend fun getPaymentDetails(@Path("paymentId") paymentId: String): PaymentResponseDto

    @GET("payments/{paymentId}/actions")
    @Headers("Content-Type: application/json")
    suspend fun getPaymentActions(@Path("paymentId") paymentId: String): List<PaymentActionDto>
}

