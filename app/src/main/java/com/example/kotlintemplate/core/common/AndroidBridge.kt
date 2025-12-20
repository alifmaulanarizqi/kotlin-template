package com.example.kotlintemplate.core.common

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
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

class AndroidBridge(
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
        val host = Uri.parse(url).host ?: return false
        return host == allowedHost
    }

    @JavascriptInterface
    fun onMessage(json: String) {
        // untuk print data indexeddb nextjs
        //  println("JS_TO_KOTLIN: $json")

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
                        println("waduhcok: ${entities.size}")
                        saveUsersLocalUseCase(entities.map { it -> it.toDomainLocal() })
                    }

                } catch (e: Exception) {

                }
            }
        }
    }
}
