package com.example.kotlintemplate.ui.feature.printer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.kotlintemplate.ui.theme.AppTheme
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.ui.ScanningActivity
import timber.log.Timber

/**
 * Activity untuk scanning dan pairing Bluetooth printer
 * Menggunakan Printooth library ScanningActivity
 */
class PrinterPairingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_PAIRED_SUCCESS = "extra_paired_success"
        const val EXTRA_PRINTER_NAME = "extra_printer_name"
        const val EXTRA_PRINTER_MAC = "extra_printer_mac"
    }

    private val requestId: String by lazy {
        intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startPrintoothScanning()
        } else {
            Timber.w("⚠️ Bluetooth permissions denied")
            finishWithResult(false)
        }
    }

    private val scanningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check if pairing was successful (Printooth returns specific result code)
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.d("✅ Printer pairing activity returned OK")

            // Check if printer is now paired
            if (Printooth.hasPairedPrinter()) {
                Timber.d("✅ Printer paired successfully")
                finishWithResult(success = true)
            } else {
                Timber.w("⚠️ Result OK but no paired printer found")
                finishWithResult(false)
            }
        } else {
            Timber.d("ℹ️ Pairing cancelled or failed (resultCode: ${result.resultCode})")
            finishWithResult(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                PrinterPairingScreen(
                    onStartScan = { checkPermissionsAndScan() },
                    onCancel = { finishWithResult(false) }
                )
            }
        }

        // Auto-start scan jika permission sudah granted
        checkPermissionsAndScan()
    }

    private fun checkPermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startPrintoothScanning()
        } else {
            Timber.d("📱 Requesting Bluetooth permissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun startPrintoothScanning() {
        Timber.d("🔍 Starting Printooth scanning activity")
        scanningLauncher.launch(
            Intent(this, ScanningActivity::class.java)
        )
    }

    private fun finishWithResult(
        success: Boolean,
        printerName: String = "",
        printerMac: String = ""
    ) {
        val intent = Intent().apply {
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_PAIRED_SUCCESS, success)
            putExtra(EXTRA_PRINTER_NAME, printerName)
            putExtra(EXTRA_PRINTER_MAC, printerMac)
        }
        setResult(if (success) RESULT_OK else RESULT_CANCELED, intent)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterPairingScreen(
    onStartScan: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Bluetooth Printer") },
                navigationIcon = {
                    TextButton(onClick = onCancel) {
                        Text("← Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🖨️",
                style = MaterialTheme.typography.displayLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Printer Bluetooth Belum Terpasang",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Silakan scan dan pair dengan printer thermal Bluetooth Anda",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔍 Scan Printer Bluetooth")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batal")
            }
        }
    }
}

