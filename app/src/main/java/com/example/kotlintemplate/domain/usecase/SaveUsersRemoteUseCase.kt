package com.example.kotlintemplate.domain.usecase

import com.example.kotlintemplate.domain.model.UserRemote
import com.example.kotlintemplate.domain.repository.UserRemoteRepository
import javax.inject.Inject

class SaveUsersRemoteUseCase @Inject constructor(
    private val repo: UserRemoteRepository
) {
    suspend operator fun invoke(userLocals: List<UserRemote>) = repo.saveUsers(userLocals)
}