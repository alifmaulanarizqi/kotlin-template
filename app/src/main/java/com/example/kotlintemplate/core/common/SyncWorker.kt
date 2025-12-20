package com.example.kotlintemplate.core.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kotlintemplate.data.local.dao.UserDao
import com.example.kotlintemplate.data.local.entity.UserEntity
import com.example.kotlintemplate.data.remote.api.UserApi
import com.example.kotlintemplate.data.remote.request.NameItemRequest
import com.example.kotlintemplate.data.remote.request.UserRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val api: UserApi,
    private val dao: UserDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            // ambil snapshot dari Room sekali saja
            val users: List<UserEntity> = dao.observeUsers().first()

            if (users.isEmpty()) return Result.success()

            // map ke request (name only)
            val request = UserRequest(users.map { NameItemRequest(it.name) })

            // POST bulk
            if(request.names.isNotEmpty()) {
                api.saveUsers(request)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

