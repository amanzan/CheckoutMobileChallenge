package com.checkout.mobilechallenge.albertomanzano.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import com.checkout.mobilechallenge.albertomanzano.data.repository.PaymentRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Singleton
    @Binds
    fun bindsPaymentRepository(
        paymentRepository: PaymentRepositoryImpl
    ): PaymentRepository
}
