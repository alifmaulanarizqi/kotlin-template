package com.example.kotlintemplate

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import com.example.kotlintemplate.ui.feature.scan.QrScanActivity
import com.example.kotlintemplate.ui.feature.scan.ScanCoordinator
import com.example.kotlintemplate.ui.feature.scan.WebViewCallbacks
import com.example.kotlintemplate.ui.navigation.AppNavGraph
import com.example.kotlintemplate.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val requestId = result.data?.getStringExtra(QrScanActivity.EXTRA_REQUEST_ID).orEmpty()
            val code = result.data?.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT) // null kalau cancel

            // kirim balik ke web lewat callback yang sudah disiapkan di WebScreen
            WebViewCallbacks.sendScanResult?.invoke(requestId, code)
        }

    private val btPermLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            // result: Map<String, Boolean>
            val scanGranted =
                result[Manifest.permission.BLUETOOTH_SCAN] == true
            val connectGranted =
                result[Manifest.permission.BLUETOOTH_CONNECT] == true

            // optional log
            println("waduh: \"SCAN=\$scanGranted CONNECT=\$connectGranted\"")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WebView.setWebContentsDebuggingEnabled(true)

        // set startScan untuk dipakai bridge dari WebView
        ScanCoordinator.startScan = { requestId ->
            val intent = Intent(this, QrScanActivity::class.java)
                .putExtra(QrScanActivity.EXTRA_REQUEST_ID, requestId)
            scanLauncher.launch(intent)
        }

        ensureBluetoothPermissions()

        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }

    private fun ensureBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val need = mutableListOf<String>()

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                need += android.Manifest.permission.BLUETOOTH_SCAN
            }

            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                need += android.Manifest.permission.BLUETOOTH_CONNECT
            }

            if (need.isNotEmpty()) {
                btPermLauncher.launch(need.toTypedArray())
            }
        }
    }
}