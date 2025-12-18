package com.example.kotlintemplate

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kotlintemplate.AndroidBridge
import com.example.kotlintemplate.ui.feature.scan.WebViewCallbacks
import timber.log.Timber

@Composable
fun WebScreen(url: String, allowedHost: String) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true

                // 🔹 UNTUK console.log()
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
                        webView = this,
                        allowedHost = allowedHost
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
}
