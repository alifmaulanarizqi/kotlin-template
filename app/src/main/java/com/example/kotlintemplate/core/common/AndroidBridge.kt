package com.example.kotlintemplate.core.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kotlintemplate.data.local.entity.UserEntity
import com.example.kotlintemplate.data.mapper.toDomainLocal
import com.example.kotlintemplate.domain.usecase.GetUserLocalUseCase
import com.example.kotlintemplate.domain.usecase.SaveUsersLocalUseCase
import com.example.kotlintemplate.ui.feature.scan.ScanCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import org.json.JSONArray

class AndroidBridge(
    private val appContext: Context,
    private val webView: WebView,
    private val allowedHost: String,
    // Inject usecase (pakai Hilt EntryPoint saat membuat AndroidBridge)
    private val saveUsersLocalUseCase: SaveUsersLocalUseCase,
    private val getUserLocalUseCase: GetUserLocalUseCase,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JavascriptInterface
    fun scanQr(requestId: String) {
        mainHandler.post {
            if (!isTrustedPageMainThread()) return@post
            if (requestId.isBlank() || requestId.length > 80) return@post

            // minta Activity buka scanner
            ScanCoordinator.startScan?.invoke(requestId)
        }
    }

    private fun isTrustedPageMainThread(): Boolean {
        val url = webView.url ?: return false
        val host = url.toUri().host ?: return false
        return host == allowedHost
    }

    @JavascriptInterface
    fun onMessage(json: String) {
        mainHandler.post {
            if (!isTrustedPageMainThread()) return@post

            // proses + Room di IO
            scope.launch {
                try {
                    val obj = JSONObject(json)
                    val type = obj.optString("type")

                    if (type != "NAMES_EXPORT") return@launch
                    val itemsArr = obj.optJSONArray("items") ?: return@launch

                    val entities = ArrayList<UserEntity>(itemsArr.length())

                    for (i in 0 until itemsArr.length()) {
                        val item = itemsArr.optJSONObject(i) ?: continue

                        val id = item.optString("id")
                        val name = item.optString("name")
                        val status = item.optString("status")

                        if (id.isBlank() || name.isBlank()) continue

                        entities.add(
                            UserEntity(
                                id = id,
                                name = name,
                                status = status,
                            )
                        )
                    }

                    if (entities.isNotEmpty()) {
                        saveUsersLocalUseCase(entities.map { it -> it.toDomainLocal() })
                        scheduleSyncWorker()
                    }

                } catch (e: Exception) {

                }
            }
        }
    }

    private fun sendToJs(requestId: String, type: String, ok: Boolean, extra: (JSONObject.() -> Unit)? = null) {
        val data = JSONObject().apply {
            put("ok", ok)
            extra?.invoke(this)
        }
        val payload = JSONObject().apply {
            put("requestId", requestId)
            put("type", type)
            put("data", data)
        }
        val script = "window.__onBridgeResult && window.__onBridgeResult(${JSONObject.quote(payload.toString())});"
        mainHandler.post { webView.evaluateJavascript(script, null) }
    }

    @JavascriptInterface
    fun btListPaired(requestId: String) {
        mainHandler.post {
            if (!isTrustedPageMainThread()) return@post
            val res = runCatching { BtPrinterClassic.listPaired(appContext).getOrThrow() }
            if (res.isFailure) {
                sendToJs(requestId, "BT_PAIRED_LIST", false) { put("error", res.exceptionOrNull()?.message ?: "error") }
                return@post
            }
            val devices = res.getOrNull().orEmpty()
            sendToJs(requestId, "BT_PAIRED_LIST", true) {
                val arr = JSONArray()
                devices.forEach { d ->
                    arr.put(JSONObject().apply {
                        put("name", d.name ?: "")
                        put("mac", d.mac)
                    })
                }
                put("devices", arr)
            }
        }
    }

    @JavascriptInterface
    fun btConnect(requestId: String, mac: String) {
        Thread {
            val r = runCatching { BtPrinterClassic.connect(appContext, mac).getOrThrow() }
            if (r.isSuccess) {
                sendToJs(requestId, "BT_CONNECT_RESULT", true)
            } else {
                sendToJs(requestId, "BT_CONNECT_RESULT", false) { put("error", r.exceptionOrNull()?.message ?: "connect error") }
            }
        }.start()
    }

    @JavascriptInterface
    fun btPrint(requestId: String, mac: String, base64: String) {
        Thread {
            val bytes = runCatching {
                android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            }.getOrNull()

            if (bytes == null) {
                sendToJs(requestId, "BT_PRINT_RESULT", false) { put("error", "Invalid base64") }
                return@Thread
            }

            val r = runCatching { BtPrinterClassic.printAndDisconnect(mac, bytes).getOrThrow() }
            if (r.isSuccess) {
                sendToJs(requestId, "BT_PRINT_RESULT", true)
            } else {
                sendToJs(requestId, "BT_PRINT_RESULT", false) { put("error", r.exceptionOrNull()?.message ?: "print error") }
            }
        }.start()
    }

    @JavascriptInterface
    fun btDisconnect(requestId: String, mac: String) {
        Thread {
            BtPrinterClassic.disconnect(mac)
            sendToJs(requestId, "BT_DISCONNECT_RESULT", true)
        }.start()
    }

    private fun scheduleSyncWorker() {
        try {
            WorkManager.getInstance(appContext)
                .enqueueUniqueWork(
                    "api_sync_flush",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<SyncWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            30, TimeUnit.SECONDS
                        )
                        .build()
                )
        } catch (e: Exception) {

        }
    }
}
