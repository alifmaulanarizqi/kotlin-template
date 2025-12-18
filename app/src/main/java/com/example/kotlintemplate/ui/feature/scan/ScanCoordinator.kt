package com.example.kotlintemplate.ui.feature.scan

object ScanCoordinator {
    var startScan: ((requestId: String) -> Unit)? = null
}