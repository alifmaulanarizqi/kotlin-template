package com.example.kotlintemplate

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.kotlintemplate.ui.feature.scan.ScanCoordinator

class AndroidBridge(
    private val webView: WebView,
    private val allowedHost: String,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun scanQr(requestId: String) {
        // PENTING: pindah ke main thread dulu
        mainHandler.post {
            if (!isTrustedPageMainThread()) return@post
            if (requestId.isBlank() || requestId.length > 80) return@post

            // minta Activity buka scanner
            ScanCoordinator.startScan?.invoke(requestId)
        }
    }

    // Fungsi ini JANGAN dipanggil dari JavaBridge thread
    private fun isTrustedPageMainThread(): Boolean {
        val url = webView.url ?: return false
        val host = Uri.parse(url).host ?: return false
        return host == allowedHost
    }
}
