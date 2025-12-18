package com.example.kotlintemplate.ui.feature.scan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class QrScanActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REQUEST_ID = "REQUEST_ID"
        const val EXTRA_SCAN_RESULT = "SCAN_RESULT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()

        setContent {
            QrScannerScreen(
                onResult = { code ->
                    setResult(
                        RESULT_OK,
                        Intent()
                            .putExtra(EXTRA_REQUEST_ID, requestId)
                            .putExtra(EXTRA_SCAN_RESULT, code)
                    )
                    finish()
                },
                onClose = {
                    // user cancel
                    setResult(
                        RESULT_CANCELED,
                        Intent().putExtra(EXTRA_REQUEST_ID, requestId)
                    )
                    finish()
                }
            )
        }
    }
}
