package com.example.kotlintemplate

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
import com.example.kotlintemplate.ui.feature.printer.PrinterPairingActivity
import com.example.kotlintemplate.ui.feature.printer.PrinterPairingCoordinator
import com.example.kotlintemplate.ui.navigation.AppNavGraph
import com.example.kotlintemplate.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val requestId = result.data?.getStringExtra(QrScanActivity.EXTRA_REQUEST_ID).orEmpty()
            val code = result.data?.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT) // null kalau cancel

            // kirim balik ke web lewat callback yang sudah disiapkan di WebScreen
            WebViewCallbacks.sendScanResult?.invoke(requestId, code)
        }

    private val printerPairingLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val requestId = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_REQUEST_ID).orEmpty()
            val success = result.data?.getBooleanExtra(PrinterPairingActivity.EXTRA_PAIRED_SUCCESS, false) ?: false
            val printerName = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_PRINTER_NAME)
            val printerMac = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_PRINTER_MAC)

            // kirim hasil pairing ke AndroidBridge
            PrinterPairingCoordinator.sendPairingResult?.invoke(requestId, success, printerName, printerMac)
        }

    private val btPermLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val allGranted = result.all { it.value }
            if (allGranted) {
                Timber.d("Bluetooth permissions granted")
            }
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

        // set startPrinterPairing untuk dipakai bridge dari WebView
        PrinterPairingCoordinator.startPrinterPairing = { requestId ->
            val intent = Intent(this, PrinterPairingActivity::class.java)
                .putExtra(PrinterPairingActivity.EXTRA_REQUEST_ID, requestId)
            printerPairingLauncher.launch(intent)
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