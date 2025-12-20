package com.example.kotlintemplate.data.remote.datasource

import com.example.kotlintemplate.data.remote.api.UserApi
import com.example.kotlintemplate.data.remote.request.UserRequest
import javax.inject.Inject

class UserRemoteDataSource @Inject constructor(
    private val api: UserApi
) {
    suspend fun saveUsers(request: UserRequest) = api.saveUsers(request)
}