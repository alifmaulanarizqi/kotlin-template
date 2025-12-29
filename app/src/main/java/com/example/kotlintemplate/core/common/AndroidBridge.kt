package com.example.kotlintemplate.core.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kotlintemplate.data.local.entity.UserEntity
import com.example.kotlintemplate.data.mapper.toDomainLocal
import com.example.kotlintemplate.domain.usecase.GetUserLocalUseCase
import com.example.kotlintemplate.domain.usecase.SaveUsersLocalUseCase
import com.example.kotlintemplate.ui.feature.scan.ScanCoordinator
import com.example.kotlintemplate.ui.feature.printer.PrinterPairingCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import androidx.work.BackoffPolicy

class AndroidBridge(
    private val appContext: Context,
    private val webView: WebView,
    private val allowedHost: String,
    // Inject usecase (pakai Hilt EntryPoint saat membuat AndroidBridge)
    private val saveUsersLocalUseCase: SaveUsersLocalUseCase,
    @Suppress("unused")
    private val getUserLocalUseCase: GetUserLocalUseCase,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Storage untuk pending print request (saat pairing)
    private val pendingPrintRequests = mutableMapOf<String, String>() // requestId -> templateJson

    init {
        // Setup callback untuk pairing result
        PrinterPairingCoordinator.sendPairingResult = { requestId, success, printerName, printerMac ->
            handlePairingResult(requestId, success, printerName, printerMac)
        }
    }

    // Note: PrintoothBridge uses lazy initialization - akan auto-init saat pertama kali print

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

                } catch (_: Exception) {
                    // Ignore JSON parsing errors
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

        // Pastikan dipanggil di main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            webView.evaluateJavascript(script, null)
        } else {
            mainHandler.post { webView.evaluateJavascript(script, null) }
        }
    }


    /**
     * Print dengan Printooth Library (support template: text, QR, raw)
     * Jika printer belum paired, akan otomatis membuka activity pairing
     */
    @JavascriptInterface
    fun btPrintWithTemplate(requestId: String, templateJson: String) {
        timber.log.Timber.d("🖨️ btPrintWithTemplate called with requestId: $requestId")
        timber.log.Timber.d("📄 Template JSON: $templateJson")

        mainHandler.post {
            if (!isTrustedPageMainThread()) return@post

            scope.launch {
                // Check if Printooth has paired printer
                val hasPaired = PrintoothBridge.hasPairedPrinter()
                timber.log.Timber.d("🔍 Printooth hasPairedPrinter: $hasPaired")

                if (!hasPaired) {
                    timber.log.Timber.w("⚠️ No paired printer found - Launching pairing activity")

                    // Simpan request untuk dieksekusi setelah pairing berhasil
                    pendingPrintRequests[requestId] = templateJson

                    // Launch pairing activity
                    PrinterPairingCoordinator.startPrinterPairing?.invoke(requestId)
                    return@launch
                }

                timber.log.Timber.d("✅ Printer paired - Starting print process")
                executePrint(requestId, templateJson)
            }
        }
    }

    /**
     * Execute print (extracted untuk reusability)
     */
    private suspend fun executePrint(requestId: String, templateJson: String) {
        val result = PrintoothBridge.printWithTemplate(templateJson)

        if (result.isSuccess) {
            timber.log.Timber.d("✅ Print successful!")
            sendToJs(requestId, "BT_PRINT_TEMPLATE_RESULT", true)
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Print gagal"
            timber.log.Timber.e("❌ Print failed: $errorMsg")
            sendToJs(requestId, "BT_PRINT_TEMPLATE_RESULT", false) {
                put("error", errorMsg)
            }
        }
    }

    /**
     * Handle hasil dari pairing activity
     */
    private fun handlePairingResult(
        requestId: String,
        success: Boolean,
        printerName: String?,
        printerMac: String?
    ) {
        timber.log.Timber.d("📱 Pairing result - success: $success, printer: $printerName ($printerMac)")

        scope.launch {
            if (success) {
                // Pairing berhasil - lanjutkan print
                val templateJson = pendingPrintRequests.remove(requestId)
                if (templateJson != null) {
                    timber.log.Timber.d("🖨️ Resuming print after successful pairing")
                    executePrint(requestId, templateJson)
                } else {
                    timber.log.Timber.w("⚠️ No pending print request found for $requestId")
                    sendToJs(requestId, "BT_PRINT_TEMPLATE_RESULT", true) {
                        put("message", "Printer paired successfully: $printerName")
                    }
                }
            } else {
                // Pairing dibatalkan/gagal
                pendingPrintRequests.remove(requestId)
                timber.log.Timber.w("⚠️ Pairing cancelled or failed")
                sendToJs(requestId, "BT_PRINT_TEMPLATE_RESULT", false) {
                    put("error", "Pairing dibatalkan. Printer belum terpasang.")
                }
            }
        }
    }

    /**
     * Check apakah printer sudah dipasangkan (paired) via Bluetooth Settings
     */
    @JavascriptInterface
    fun btHasPairedPrinter(requestId: String) {
        mainHandler.post {
            val hasPaired = PrintoothBridge.hasPairedPrinter()
            sendToJs(requestId, "BT_HAS_PAIRED_RESULT", true) {
                put("hasPaired", hasPaired)
            }
        }
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
        } catch (_: Exception) {
            // Ignore WorkManager errors
        }
    }
}
