package com.example.kotlintemplate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kotlintemplate.core.common.SyncWorker
import com.example.kotlintemplate.ui.feature.scan.QrScanActivity
import com.example.kotlintemplate.ui.feature.scan.ScanCoordinator
import com.example.kotlintemplate.ui.feature.scan.WebViewCallbacks
import com.example.kotlintemplate.ui.navigation.AppNavGraph
import com.example.kotlintemplate.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val scanLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val requestId = result.data?.getStringExtra(QrScanActivity.EXTRA_REQUEST_ID).orEmpty()
            val code = result.data?.getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT) // null kalau cancel

            // kirim balik ke web lewat callback yang sudah disiapkan di WebScreen
            WebViewCallbacks.sendScanResult?.invoke(requestId, code)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSync(this)

        WebView.setWebContentsDebuggingEnabled(true)

        // set startScan untuk dipakai bridge dari WebView
        ScanCoordinator.startScan = { requestId ->
            val intent = Intent(this, QrScanActivity::class.java)
                .putExtra(QrScanActivity.EXTRA_REQUEST_ID, requestId)
            scanLauncher.launch(intent)
        }

        setContent {
            AppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }

    fun scheduleSync(context: Context) {
        val test = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(this).enqueue(test)
//        val request =
//            PeriodicWorkRequestBuilder<SyncWorker>(
//                15, TimeUnit.MINUTES
//            ).setConstraints(
//                    Constraints.Builder()
//                        .setRequiredNetworkType(NetworkType.CONNECTED)
//                        .build()
//                )
//                .build()
//
//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//            "periodic_api_sync",
//            ExistingPeriodicWorkPolicy.KEEP,
//            request
//        )
    }

}