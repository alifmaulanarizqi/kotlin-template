package com.example.kotlintemplate.di

import com.example.kotlintemplate.data.repository.SampleRepositoryImpl
import com.example.kotlintemplate.data.repository.UserLocalRepositoryImpl
import com.example.kotlintemplate.domain.repository.SampleRepository
import com.example.kotlintemplate.domain.repository.UserLocalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSampleRepository(
        impl: SampleRepositoryImpl
    ): SampleRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        impl: UserLocalRepositoryImpl
    ): UserLocalRepository
}