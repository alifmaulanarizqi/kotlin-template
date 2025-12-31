package com.example.kotlintemplate

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotlintemplate.core.common.AndroidBridge
import com.example.kotlintemplate.core.common.BridgeEntryPoint
import com.example.kotlintemplate.ui.feature.scan.WebViewCallbacks
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, allowedHost: String) {
    val appContext = LocalContext.current.applicationContext
    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
        appContext,
        BridgeEntryPoint::class.java
    )

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true

                // buat nampilin alert dari js
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        Timber.tag("WEB_CONSOLE").d(
                            "${consoleMessage.message()} " +
                                    "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
                        )
                        return true
                    }
                }

                webViewClient = WebViewClient()

                addJavascriptInterface(
                    AndroidBridge(
                        appContext = ctx.applicationContext,
                        webView = this,
                        allowedHost = allowedHost,
                        saveUsersLocalUseCase = entryPoint.saveScanUseCase(),
                        getUserLocalUseCase = entryPoint.getScansUseCase(),
                    ),
                    "AndroidBridge"
                )

                loadUrl(url)
            }
        },
        update = { webView ->
            // register callback untuk kirim result dari Activity ke WebView ini
            WebViewCallbacks.sendScanResult = { requestId, code ->
                fun esc(s: String) = s
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

                val rid = esc(requestId)
                val value = code?.let { "'${esc(it)}'" } ?: "null"

                val js = "window.__onNativeScanResult && window.__onNativeScanResult('$rid', $value);"
                webView.post { webView.evaluateJavascript(js, null) }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            WebViewCallbacks.sendScanResult = null
        }
    }
}
