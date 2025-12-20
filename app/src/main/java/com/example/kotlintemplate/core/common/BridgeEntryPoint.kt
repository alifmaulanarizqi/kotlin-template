package com.example.kotlintemplate.core.common

import com.example.kotlintemplate.domain.usecase.GetUserLocalUseCase
import com.example.kotlintemplate.domain.usecase.SaveUsersLocalUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BridgeEntryPoint {
    fun saveScanUseCase(): SaveUsersLocalUseCase
    fun getScansUseCase(): GetUserLocalUseCase
}