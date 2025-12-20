package com.example.kotlintemplate.domain.repository

import com.example.kotlintemplate.domain.model.UserRemote

interface UserRemoteRepository {
    suspend fun saveUsers(users: List<UserRemote>) : List<UserRemote>
}