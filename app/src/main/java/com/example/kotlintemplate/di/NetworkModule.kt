package com.example.kotlintemplate.di

import com.example.kotlintemplate.BuildConfig
import com.example.kotlintemplate.data.remote.api.SampleApi
import com.example.kotlintemplate.data.remote.api.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
//            .baseUrl(BuildConfig.BASE_URL)
            .baseUrl("http://127.0.0.1:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSampleApi(
        retrofit: Retrofit
    ): SampleApi =
        retrofit.create(SampleApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(
        retrofit: Retrofit
    ): UserApi =
        retrofit.create(UserApi::class.java)
}