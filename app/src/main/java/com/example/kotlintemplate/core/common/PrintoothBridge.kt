package com.example.kotlintemplate.core.common

import android.graphics.Bitmap
import com.mazenrashed.printooth.Printooth
import com.mazenrashed.printooth.data.printer.DefaultPrinter
import com.mazenrashed.printooth.data.printable.ImagePrintable
import com.mazenrashed.printooth.data.printable.Printable
import com.mazenrashed.printooth.data.printable.RawPrintable
import com.mazenrashed.printooth.data.printable.TextPrintable
import com.mazenrashed.printooth.utilities.Printing
import com.mazenrashed.printooth.utilities.PrintingCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import net.glxn.qrgen.android.QRCode
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Wrapper untuk Printooth Library
 * Digunakan oleh AndroidBridge untuk print ke thermal printer
 * 
 * Note: Menggunakan lazy initialization - tidak perlu init manual
 */
object PrintoothBridge {
    @Volatile
    @Suppress("StaticFieldLeak") // Printooth library design - unavoidable
    private var printing: Printing? = null
    
    private var initialized = false

    /**
     * Lazy initialization - hanya init saat pertama kali digunakan
     * Akan dipanggil otomatis saat print pertama kali
     */
    private fun ensureInitialized() {
        if (initialized) return
        
        initialized = true
        
        if (Printooth.hasPairedPrinter()) {
            printing = Printooth.printer()
            Timber.d("✅ Printooth initialized - Printer found and ready")
        } else {
            // Ini bukan error, hanya informasi
            // User belum pair printer via Settings > Bluetooth
            Timber.i("ℹ️ Printer belum dipasangkan - Pair printer via Settings > Bluetooth terlebih dahulu")
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun hasPairedPrinter(): Boolean {
        val hasPaired = Printooth.hasPairedPrinter()
        Timber.d("🔍 Checking Printooth.hasPairedPrinter(): $hasPaired")

        // Debug: Juga cek Bluetooth bonded devices
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                val bondedDevices = bluetoothAdapter.bondedDevices
                Timber.d("📱 Bluetooth bonded devices count: ${bondedDevices?.size ?: 0}")
                bondedDevices?.forEach { device ->
                    Timber.d("  - ${device.name} (${device.address})")
                }
            } else {
                Timber.w("⚠️ Bluetooth adapter is null or disabled")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking Bluetooth devices")
        }

        return hasPaired
    }

    /**
     * Print dengan template JSON dari WebView
     * Format JSON:
     * {
     *   "items": [
     *     { "type": "text", "text": "Hello", "alignment": "center", "fontSize": "large", ... },
     *     { "type": "qr", "data": "QR Content", "size": 200 },
     *     { "type": "raw", "bytes": [27, 100, 4] }
     *   ]
     * }
     */
    suspend fun printWithTemplate(templateJson: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            // Lazy init - hanya init saat pertama kali print
            ensureInitialized()
            
            val json = JSONObject(templateJson)
            val items = json.getJSONArray("items")

            val printables = parsePrintables(items)

            if (printables.isEmpty()) {
                continuation.resume(Result.failure(IllegalArgumentException("No printable items found")))
                return@suspendCancellableCoroutine
            }

            // Get printer instance
            val printer = printing ?: run {
                // Coba re-init jika printer baru saja di-pair setelah app start
                if (Printooth.hasPairedPrinter()) {
                    printing = Printooth.printer()
                    printing
                } else {
                    continuation.resume(Result.failure(IllegalStateException("Printer belum dipasangkan. Pair printer via Settings > Bluetooth")))
                    return@suspendCancellableCoroutine
                }
            } ?: run {
                continuation.resume(Result.failure(IllegalStateException("Failed to get printer instance")))
                return@suspendCancellableCoroutine
            }

            var callbackInvoked = false

            // Setup callback untuk handle result dari Printooth
            printer.printingCallback = object : PrintingCallback {
                override fun connectingWithPrinter() {
                    Timber.d("🔗 Connecting to printer...")
                }

                override fun printingOrderSentSuccessfully() {
                    Timber.d("✅ Print successful!")
                    if (!callbackInvoked) {
                        callbackInvoked = true
                        continuation.resume(Result.success(Unit))
                    }
                }

                override fun connectionFailed(error: String) {
                    Timber.e("❌ Connection failed: $error")
                    if (!callbackInvoked) {
                        callbackInvoked = true
                        continuation.resume(Result.failure(Exception("Connection failed: $error")))
                    }
                }

                override fun onError(error: String) {
                    Timber.e("❌ Print error: $error")
                    if (!callbackInvoked) {
                        callbackInvoked = true
                        continuation.resume(Result.failure(Exception("Print error: $error")))
                    }
                }

                override fun onMessage(message: String) {
                    Timber.d("📝 Printer message: $message")
                }

                override fun disconnected() {
                    Timber.d("🔌 Printer disconnected")
                }
            }

            // Execute print
            Timber.d("🖨️ Sending ${printables.size} item(s) to printer...")
            printer.print(printables)

            // Timeout handler
            continuation.invokeOnCancellation {
                if (!callbackInvoked) {
                    Timber.w("⚠️ Print operation cancelled")
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Error in printWithTemplate")
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * Parse JSON array menjadi list of Printable objects
     */
    private fun parsePrintables(items: JSONArray): ArrayList<Printable> {
        val printables = ArrayList<Printable>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val type = item.optString("type", "")

            when (type) {
                "text" -> {
                    val textPrintable = parseTextPrintable(item)
                    if (textPrintable != null) {
                        printables.add(textPrintable)
                    }
                }
                "qr" -> {
                    val qrPrintable = parseQRPrintable(item)
                    if (qrPrintable != null) {
                        printables.add(qrPrintable)
                    }
                }
                "raw" -> {
                    val rawPrintable = parseRawPrintable(item)
                    if (rawPrintable != null) {
                        printables.add(rawPrintable)
                    }
                }
                "image" -> {
                    // TODO: Support image from base64 if needed
                    Timber.w("⚠️ Image type not yet supported")
                }
            }
        }

        return printables
    }

    private fun parseTextPrintable(item: JSONObject): TextPrintable? {
        val text = item.optString("text", "")
        if (text.isEmpty()) return null

        val builder = TextPrintable.Builder().setText(text)

        // Alignment
        when (item.optString("alignment", "left").lowercase()) {
            "center" -> builder.setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
            "right" -> builder.setAlignment(DefaultPrinter.ALIGNMENT_RIGHT)
            else -> builder.setAlignment(DefaultPrinter.ALIGNMENT_LEFT)
        }

        // Font size
        when (item.optString("fontSize", "normal").lowercase()) {
            "large" -> builder.setFontSize(DefaultPrinter.FONT_SIZE_LARGE)
            else -> builder.setFontSize(DefaultPrinter.FONT_SIZE_NORMAL)
        }

        // Bold/Emphasized
        if (item.optBoolean("bold", false)) {
            builder.setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_BOLD)
        } else {
            builder.setEmphasizedMode(DefaultPrinter.EMPHASIZED_MODE_NORMAL)
        }

        // Underline
        if (item.optBoolean("underline", false)) {
            builder.setUnderlined(DefaultPrinter.UNDERLINED_MODE_ON)
        } else {
            builder.setUnderlined(DefaultPrinter.UNDERLINED_MODE_OFF)
        }

        // Line spacing
        val lineSpacing = item.optInt("lineSpacing", DefaultPrinter.LINE_SPACING_30.toInt())
        builder.setLineSpacing(lineSpacing.toByte())

        // New lines after
        val newLines = item.optInt("newLinesAfter", 0)
        if (newLines > 0) {
            builder.setNewLinesAfter(newLines)
        }

        // Character code
        val charCode = item.optInt("charCode", DefaultPrinter.CHARCODE_PC1252.toInt())
        builder.setCharacterCode(charCode.toByte())

        return builder.build() as TextPrintable
    }

    private fun parseQRPrintable(item: JSONObject): ImagePrintable? {
        val data = item.optString("data", "")
        if (data.isEmpty()) return null

        val size = item.optInt("size", 200)

        val qrBitmap: Bitmap = QRCode.from(data)
            .withSize(size, size)
            .bitmap()

        val builder = ImagePrintable.Builder(qrBitmap)

        // Alignment
        when (item.optString("alignment", "center").lowercase()) {
            "left" -> builder.setAlignment(DefaultPrinter.ALIGNMENT_LEFT)
            "right" -> builder.setAlignment(DefaultPrinter.ALIGNMENT_RIGHT)
            else -> builder.setAlignment(DefaultPrinter.ALIGNMENT_CENTER)
        }

        return builder.build()
    }

    private fun parseRawPrintable(item: JSONObject): RawPrintable? {
        val bytesArray = item.optJSONArray("bytes") ?: return null

        val bytes = ByteArray(bytesArray.length())
        for (i in 0 until bytesArray.length()) {
            bytes[i] = bytesArray.getInt(i).toByte()
        }

        return RawPrintable.Builder(bytes).build()
    }
}

