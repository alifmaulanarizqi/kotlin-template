package com.example.kotlintemplate.domain.repository

import com.example.kotlintemplate.data.remote.response.UserResponse
import com.example.kotlintemplate.domain.model.UserRemote

interface UserRemoteRepository {
    suspend fun saveUsers(users: List<UserRemote>) : UserResponse
}