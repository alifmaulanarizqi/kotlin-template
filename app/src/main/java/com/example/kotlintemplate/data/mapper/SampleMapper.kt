package com.example.kotlintemplate.data.mapper

import com.example.kotlintemplate.data.remote.response.SampleResponse
import com.example.kotlintemplate.domain.model.Sample

fun List<SampleResponse>.toDomainLocal(): List<Sample> =
    map { Sample(id = it.id, title = it.title) }