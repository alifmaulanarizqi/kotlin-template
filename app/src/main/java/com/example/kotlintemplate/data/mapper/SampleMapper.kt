package com.example.kotlintemplate.data.mapper

import com.example.kotlintemplate.data.remote.dto.SampleResponse
import com.example.kotlintemplate.domain.model.Sample

fun List<SampleResponse>.toDomain(): List<Sample> =
    map { Sample(id = it.id, title = it.title) }