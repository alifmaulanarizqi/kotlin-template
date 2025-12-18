package com.example.kotlintemplate.ui.feature.scan

object WebViewCallbacks {
    var sendScanResult: ((requestId: String, code: String?) -> Unit)? = null
}