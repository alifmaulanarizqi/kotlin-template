package com.example.kotlintemplate.domain.usecase

import com.example.kotlintemplate.domain.model.UserLocal
import com.example.kotlintemplate.domain.repository.UserLocalRepository
import javax.inject.Inject

class SaveUsersLocalUseCase @Inject constructor(
    private val repo: UserLocalRepository
) {
    suspend operator fun invoke(userLocals: List<UserLocal>) = repo.saveUsers(userLocals)
}
