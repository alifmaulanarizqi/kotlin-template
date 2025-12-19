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
import com.example.kotlintemplate.ui.feature.scan.WebViewCallbacks
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, allowedHost: String) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                WebView.setWebContentsDebuggingEnabled(true)

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

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)

                        // ✅ expose helper function di window biar bisa dipanggil dari Kotlin kapan pun
                        // (Tidak memaksa jalan otomatis; cuma menambahkan hook)
                        val js = """
                            (function() {
                              if (window.__nativeRequestIndexedDbExport) return;

                              window.__nativeRequestIndexedDbExport = async function() {
                                try {
                                  // kamu boleh ganti function name ini sesuai yang kamu buat di Next.js
                                  // contoh: window.exportIndexedDbToAndroid()
                                  if (window.exportIndexedDbToAndroid) {
                                    await window.exportIndexedDbToAndroid();
                                    return "OK";
                                  }
                                  return "exportIndexedDbToAndroid_not_found";
                                } catch (e) {
                                  return "ERR:" + (e && e.message ? e.message : String(e));
                                }
                              };
                            })();
                        """.trimIndent()

                        view.evaluateJavascript(js, null)
                    }
                }

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

            // ✅ OPTIONAL: kalau kamu mau Kotlin yang "minta" export IndexedDB kapan pun:
            // panggil ini dari tempat lain: webView.evaluateJavascript("window.__nativeRequestIndexedDbExport && window.__nativeRequestIndexedDbExport()", null)
        }
    )

    // ✅ Optional cleanup (tidak mengubah logic existing, hanya mencegah leak)
    DisposableEffect(Unit) {
        onDispose {
            WebViewCallbacks.sendScanResult = null
        }
    }
}
