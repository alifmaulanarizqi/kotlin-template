package com.example.kotlintemplate.ui.feature.home

import com.example.kotlintemplate.domain.model.Sample

data class HomeState(
    val loading: Boolean = false,
    val samples: List<Sample> = emptyList(),
    val error: String? = null
)