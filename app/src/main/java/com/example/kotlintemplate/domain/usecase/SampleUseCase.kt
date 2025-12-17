package com.example.kotlintemplate.domain.usecase

import com.example.kotlintemplate.domain.model.Sample
import com.example.kotlintemplate.domain.repository.SampleRepository
import javax.inject.Inject

class SampleUseCase @Inject constructor(
    private val repository: SampleRepository
) {
    suspend operator fun invoke(): List<Sample> =
        repository.getSample()
}