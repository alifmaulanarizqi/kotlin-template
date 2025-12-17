package com.example.kotlintemplate.domain.repository

import com.example.kotlintemplate.domain.model.Sample

interface SampleRepository {
    suspend fun getSample(): List<Sample>
}