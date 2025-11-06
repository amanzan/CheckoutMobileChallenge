package com.checkout.mobilechallenge.albertomanzano.di

import com.checkout.mobilechallenge.albertomanzano.data.remote.api.CheckoutApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Public key for tokenization
    private const val PUBLIC_KEY = "pk_sbox_gnrjo6pl5azfmgdnrfrbbejo7ev"
    // Private key for payments
    private const val PRIVATE_KEY = "sk_sbox_bvzfhwhivsgi33smfjjeb6t64i4"
    private const val CHECKOUT_BASE_URL = "https://api.sandbox.checkout.com/"

    @Provides
    @Singleton
    @Named("checkout_token")
    fun provideCheckoutTokenRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", PUBLIC_KEY)
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("checkout_payment")
    fun provideCheckoutPaymentRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", PRIVATE_KEY)
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(CHECKOUT_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("checkout_token_api")
    fun provideCheckoutTokenApi(@Named("checkout_token") retrofit: Retrofit): CheckoutApi =
        retrofit.create(CheckoutApi::class.java)

    @Provides
    @Singleton
    @Named("checkout_payment_api")
    fun provideCheckoutPaymentApi(@Named("checkout_payment") retrofit: Retrofit): CheckoutApi =
        retrofit.create(CheckoutApi::class.java)
}
