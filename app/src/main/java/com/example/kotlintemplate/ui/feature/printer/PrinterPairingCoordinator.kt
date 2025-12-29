package com.example.kotlintemplate.ui.feature.printer

/**
 * Coordinator untuk launch PrinterPairingActivity
 * Mirip dengan ScanCoordinator untuk QR Scanner
 */
object PrinterPairingCoordinator {
    var startPrinterPairing: ((requestId: String) -> Unit)? = null
    var sendPairingResult: ((requestId: String, success: Boolean, printerName: String?, printerMac: String?) -> Unit)? = null
}

