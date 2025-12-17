package com.example.kotlintemplate.data.remote.datasource

import com.example.kotlintemplate.data.remote.api.SampleApi
import com.example.kotlintemplate.data.remote.dto.SampleResponse
import javax.inject.Inject

class SampleRemoteDataSource @Inject constructor(
    private val api: SampleApi
) {
    suspend fun getSample(): List<SampleResponse> = api.getSample()
}