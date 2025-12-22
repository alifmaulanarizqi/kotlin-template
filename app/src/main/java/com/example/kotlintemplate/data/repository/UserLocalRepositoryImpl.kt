package com.example.kotlintemplate.data.repository

import com.example.kotlintemplate.data.local.dao.UserDao
import com.example.kotlintemplate.data.mapper.toDomainLocal
import com.example.kotlintemplate.data.mapper.toEntityLocal
import com.example.kotlintemplate.domain.model.UserLocal
import com.example.kotlintemplate.domain.repository.UserLocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserLocalRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserLocalRepository {

    override suspend fun observeUsers(): List<UserLocal> {
        return userDao.observeUsers().map { it.toDomainLocal() }
    }

    override suspend fun saveUsers(userLocals: List<UserLocal>) {
        userDao.upsertAll(userLocals.map { it.toEntityLocal() })
    }

    override suspend fun clearUsers() {
        userDao.clearAll()
    }
}