package com.example.kotlintemplate.core.common

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
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
            while (true) {
                val users = dao.observeUsers(limit = 1)
                if (users.isEmpty()) break

                val request = UserRequest(users.map { NameItemRequest(it.name) })

                api.saveUsers(request)

                dao.deleteByIds(users.map { it.id })
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

