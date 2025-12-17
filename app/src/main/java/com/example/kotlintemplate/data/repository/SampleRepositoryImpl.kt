package com.example.kotlintemplate.data.repository

import com.example.kotlintemplate.data.mapper.toDomain
import com.example.kotlintemplate.data.remote.datasource.SampleRemoteDataSource
import com.example.kotlintemplate.data.remote.dto.SampleResponse
import com.example.kotlintemplate.domain.model.Sample
import com.example.kotlintemplate.domain.repository.SampleRepository
import javax.inject.Inject

class SampleRepositoryImpl @Inject constructor(
    private val remote: SampleRemoteDataSource
) : SampleRepository {
    override suspend fun getSample(): List<Sample> {
        val response = remote.getSample()
        return response.toDomain()
    }
}