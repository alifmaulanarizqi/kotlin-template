package com.example.kotlintemplate.data.remote.api

import com.example.kotlintemplate.data.remote.request.UserRequest
import com.example.kotlintemplate.data.remote.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface UserApi {
    @GET("api/names")
    suspend fun getUsers(): List<UserResponse>

    @POST("api/names-bulk")
    suspend fun saveUsers(
        @Body users: List<UserRequest>
    ): List<UserResponse>
}