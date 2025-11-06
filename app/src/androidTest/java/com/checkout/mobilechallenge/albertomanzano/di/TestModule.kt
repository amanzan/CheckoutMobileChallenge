package com.checkout.mobilechallenge.albertomanzano.di

import com.checkout.mobilechallenge.albertomanzano.data.remote.api.CheckoutApi
import com.checkout.mobilechallenge.albertomanzano.data.repository.PaymentRepositoryImpl
import com.checkout.mobilechallenge.albertomanzano.domain.repository.PaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test module for replacing production dependencies with test doubles.
 * This module is only used during instrumentation tests.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestModule {

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository
}

