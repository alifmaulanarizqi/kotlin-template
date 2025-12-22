package com.example.kotlintemplate.domain.repository

import com.example.kotlintemplate.domain.model.UserLocal
import kotlinx.coroutines.flow.Flow

interface UserLocalRepository {
    suspend fun observeUsers(): List<UserLocal>
    suspend fun saveUsers(userLocals: List<UserLocal>)
    suspend fun clearUsers()
}