package com.example.kotlintemplate.data.remote.api

import com.example.kotlintemplate.data.remote.dto.SampleResponse
import retrofit2.http.GET

interface SampleApi {
    @GET("sampeapi")
    suspend fun getSample(): List<SampleResponse>
}
