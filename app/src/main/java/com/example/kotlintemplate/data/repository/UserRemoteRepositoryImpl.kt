package com.example.kotlintemplate.data.repository

import com.example.kotlintemplate.data.remote.datasource.UserRemoteDataSource
import com.example.kotlintemplate.data.remote.request.NameItemRequest
import com.example.kotlintemplate.data.remote.request.UserRequest
import com.example.kotlintemplate.data.remote.response.UserResponse
import com.example.kotlintemplate.domain.model.UserRemote
import com.example.kotlintemplate.domain.repository.UserRemoteRepository
import javax.inject.Inject

class UserRemoteRepositoryImpl @Inject constructor(
    private val remote: UserRemoteDataSource
) : UserRemoteRepository {

    override suspend fun saveUsers(users: List<UserRemote>) : UserResponse {
        val nameItems : ArrayList<NameItemRequest> = ArrayList()
        users.forEach {
            nameItems.add(NameItemRequest(it.name))
        }

        val body = UserRequest(nameItems)

        val response = remote.saveUsers(body)
        return response
    }
}