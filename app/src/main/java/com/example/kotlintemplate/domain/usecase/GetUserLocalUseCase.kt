package com.example.kotlintemplate.domain.usecase

import com.example.kotlintemplate.domain.repository.UserLocalRepository
import javax.inject.Inject

class GetUserLocalUseCase @Inject constructor(
    private val repo: UserLocalRepository
) {
    suspend operator fun invoke() = repo.observeUsers()
}