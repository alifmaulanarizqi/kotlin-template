package com.example.kotlintemplate.data.repository

import com.example.kotlintemplate.data.mapper.toDomainLocal
import com.example.kotlintemplate.data.mapper.toRequest
import com.example.kotlintemplate.data.remote.datasource.UserRemoteDataSource
import com.example.kotlintemplate.domain.model.UserRemote
import com.example.kotlintemplate.domain.repository.UserRemoteRepository
import javax.inject.Inject

class UserRemoteRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource
) : UserRemoteRepository {

    override suspend fun saveUsers(users: List<UserRemote>) : List<UserRemote> {
        val responses = remote.saveUsers(users.map { it.toRequest()})
        return responses.map { it.toDomainLocal() }
    }
}