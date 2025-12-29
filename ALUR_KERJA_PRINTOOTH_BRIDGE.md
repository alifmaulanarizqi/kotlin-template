# 📋 Alur Kerja Printooth Bridge untuk Thermal Printer

## 🎯 Overview
Sistem ini memungkinkan aplikasi Next.js di WebView untuk print ke printer thermal Bluetooth menggunakan Printooth library melalui Android Bridge.

---

## 🔄 Alur Lengkap: Dari Next.js ke Printer

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         NEXT.JS WEBVIEW (Frontend)                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 1. User klik "Print"
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  btPrintWithTemplate(templateJson)             │
        │  - Function di android-printer-bridge.ts      │
        └────────────────────────────────────────────────┘
                                    │
                                    │ 2. Generate requestId (UUID)
                                    │ 3. Stringify JSON template
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  window.AndroidBridge.btPrintWithTemplate()   │
        │  - JavaScript Interface                        │
        └────────────────────────────────────────────────┘
                                    │
                    ════════════════════════════════════
                    ║   BRIDGE LAYER (WebView ↔ Native) ║
                    ════════════════════════════════════
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      ANDROID NATIVE (Backend)                           │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 4. @JavascriptInterface receives call
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  AndroidBridge.btPrintWithTemplate()           │
        │  - File: AndroidBridge.kt                      │
        └────────────────────────────────────────────────┘
                                    │
                                    │ 5. Check if printer paired
                                    ▼
                    ┌───────────────────────────┐
                    │ PrintoothBridge           │
                    │ .hasPairedPrinter()?      │
                    └───────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │                               │
                 NO │                            YES│
                    ▼                               ▼
        ┌──────────────────────┐      ┌────────────────────────┐
        │ Return error:        │      │ PrintoothBridge        │
        │ "Printer belum       │      │ .printWithTemplate()   │
        │  dipasangkan"        │      └────────────────────────┘
        └──────────────────────┘                  │
                    │                             │ 6. Parse JSON template
                    │                             ▼
                    │              ┌────────────────────────────┐
                    │              │ parsePrintables()          │
                    │              │ - Parse JSON ke Printable  │
                    │              └────────────────────────────┘
                    │                             │
                    │                             │ 7. Loop items
                    │                             ▼
                    │              ┌─────────────────────────────────┐
                    │              │  For each item in template:     │
                    │              │  - type: "text" → TextPrintable │
                    │              │  - type: "qr" → ImagePrintable  │
                    │              │  - type: "raw" → RawPrintable   │
                    │              └─────────────────────────────────┘
                    │                             │
                    │                             │ 8. Create printables list
                    │                             ▼
                    │              ┌────────────────────────────┐
                    │              │ Get Printooth printer      │
                    │              │ instance                   │
                    │              └────────────────────────────┘
                    │                             │
                    │                             │ 9. Setup callbacks
                    │                             ▼
                    │              ┌────────────────────────────────────┐
                    │              │ PrintingCallback:                  │
                    │              │ - connectingWithPrinter()          │
                    │              │ - printingOrderSentSuccessfully()  │
                    │              │ - connectionFailed()               │
                    │              │ - onError()                        │
                    │              └────────────────────────────────────┘
                    │                             │
                    │                             │ 10. Execute print
                    │                             ▼
                    │              ┌────────────────────────────┐
                    │              │ printer.print(printables)  │
                    │              └────────────────────────────┘
                    │                             │
                    │              ┌──────────────┴──────────────┐
                    │              │                             │
                    │           SUCCESS                       FAILED
                    │              │                             │
                    │              ▼                             ▼
                    │   ┌──────────────────┐         ┌──────────────────┐
                    │   │ Callback:        │         │ Callback:        │
                    │   │ Success          │         │ Error/Failed     │
                    │   └──────────────────┘         └──────────────────┘
                    │              │                             │
                    └──────────────┴─────────────────────────────┘
                                    │
                                    │ 11. Send response to WebView
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  sendToJs(requestId, type, ok, data)           │
        │  - Build JSON response                         │
        └────────────────────────────────────────────────┘
                                    │
                                    │ 12. Execute JavaScript
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  webView.evaluateJavascript()                  │
        │  window.__onBridgeResult(response)             │
        └────────────────────────────────────────────────┘
                                    │
                    ════════════════════════════════════
                    ║   BRIDGE LAYER (Native → WebView) ║
                    ════════════════════════════════════
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         NEXT.JS WEBVIEW (Frontend)                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ 13. Callback handler receives response
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  window.__onBridgeResult(payload)              │
        │  - Parse JSON response                         │
        └────────────────────────────────────────────────┘
                                    │
                                    │ 14. Find pending request by ID
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  window.__bridgePending.get(requestId)         │
        │  - Resolve Promise                             │
        └────────────────────────────────────────────────┘
                                    │
                                    │ 15. Return result to caller
                                    ▼
        ┌────────────────────────────────────────────────┐
        │  const result = await btPrintWithTemplate()    │
        │  if (result.ok) { ... }                        │
        └────────────────────────────────────────────────┘
```

---

## 📦 Komponen Utama

### 1. **Next.js Frontend** (`android-printer-bridge.ts`)
**Fungsi:**
- Expose API yang mudah digunakan untuk developer
- Handle komunikasi dengan Android Bridge
- Manage async promises dengan requestId

**Key Functions:**
```typescript
btHasPairedPrinter()      // Cek status printer
btPrintWithTemplate()     // Print dengan template
printSimpleText()         // Helper: print text
printReceipt()            // Helper: print receipt
printWithQR()             // Helper: print dengan QR
```

### 2. **Android Bridge** (`AndroidBridge.kt`)
**Fungsi:**
- Bridge antara WebView dan Native code
- Expose native functions ke JavaScript
- Handle security (check trusted page)
- Send results back to WebView

**Key Methods:**
```kotlin
@JavascriptInterface
fun btPrintWithTemplate(requestId: String, templateJson: String)

@JavascriptInterface
fun btHasPairedPrinter(requestId: String)
```

### 3. **Printooth Bridge** (`PrintoothBridge.kt`)
**Fungsi:**
- Wrapper untuk Printooth library
- Parse JSON template ke Printable objects
- Handle printer callbacks
- Generate QR codes

**Key Methods:**
```kotlin
fun initPrintooth()
fun hasPairedPrinter(): Boolean
suspend fun printWithTemplate(templateJson: String): Result<Unit>
```

### 4. **Printooth Library** (Third-party)
**Fungsi:**
- Komunikasi Bluetooth dengan printer
- Format ESC/POS commands
- Handle koneksi dan print queue

---

## 🔍 Detail Alur per Komponen

### A. Inisialisasi (Saat App Start)

```
MainActivity.onCreate()
       │
       ▼
AndroidBridge constructor
       │
       ▼
PrintoothBridge.initPrintooth()
       │
       ▼
Printooth.hasPairedPrinter() → Check Settings Bluetooth
       │
       ├─ YES → Printooth.printer() → Save instance
       └─ NO  → Log warning
```

### B. Print Flow (User Action)

#### Step 1-3: Next.js Request
```typescript
// User code
await btPrintWithTemplate({
  items: [
    { type: "text", text: "Hello", alignment: "center" },
    { type: "qr", data: "QR123", size: 200 }
  ]
});

↓

// Bridge helper
const requestId = crypto.randomUUID();  // "550e8400-..."
const template = JSON.stringify(items);
window.AndroidBridge.btPrintWithTemplate(requestId, template);
```

#### Step 4-5: Bridge Receives Call
```kotlin
@JavascriptInterface
fun btPrintWithTemplate(requestId: String, templateJson: String) {
    scope.launch {
        // Check printer paired
        if (!PrintoothBridge.hasPairedPrinter()) {
            sendToJs(requestId, "BT_PRINT_TEMPLATE_RESULT", false) {
                put("error", "Printer belum dipasangkan")
            }
            return@launch
        }
        // Continue...
    }
}
```

#### Step 6-8: Parse Template
```kotlin
// Parse JSON string
val json = JSONObject(templateJson)
val items = json.getJSONArray("items")

// Loop items
for (i in 0 until items.length()) {
    val item = items.getJSONObject(i)
    val type = item.optString("type")
    
    when (type) {
        "text" → parseTextPrintable(item)  // TextPrintable.Builder()
        "qr"   → parseQRPrintable(item)    // QRCode.from().bitmap()
        "raw"  → parseRawPrintable(item)   // RawPrintable.Builder()
    }
}
```

#### Step 9-10: Execute Print
```kotlin
// Setup callback
printer.printingCallback = object : PrintingCallback {
    override fun printingOrderSentSuccessfully() {
        continuation.resume(Result.success(Unit))
    }
    override fun onError(error: String) {
        continuation.resume(Result.failure(Exception(error)))
    }
    // ... other callbacks
}

// Execute print (async)
printer.print(printables)
```

#### Step 11-12: Send Response
```kotlin
fun sendToJs(requestId: String, type: String, ok: Boolean, extra: {...}) {
    val payload = JSONObject().apply {
        put("requestId", requestId)
        put("type", type)
        put("data", JSONObject().apply {
            put("ok", ok)
            extra?.invoke(this)
        })
    }
    
    val script = """
        window.__onBridgeResult(${JSONObject.quote(payload.toString())});
    """
    
    webView.evaluateJavascript(script, null)
}
```

#### Step 13-15: Resolve Promise
```typescript
window.__onBridgeResult = (payload: string) => {
    const msg = JSON.parse(payload);
    const callback = window.__bridgePending.get(msg.requestId);
    
    if (callback) {
        window.__bridgePending.delete(msg.requestId);
        callback(msg);  // Resolve Promise
    }
}
```

---

## 🎨 Contoh Template Parsing

### Input JSON:
```json
{
  "items": [
    {
      "type": "text",
      "text": "TOKO SAYA",
      "alignment": "center",
      "fontSize": "large",
      "bold": true,
      "newLinesAfter": 2
    },
    {
      "type": "qr",
      "data": "ORDER#12345",
      "size": 200,
      "alignment": "center"
    },
    {
      "type": "raw",
      "bytes": [27, 100, 4]
    }
  ]
}
```

### Output Printables:
```kotlin
ArrayList<Printable> = [
    TextPrintable(
        text = "TOKO SAYA",
        alignment = ALIGNMENT_CENTER,
        fontSize = FONT_SIZE_LARGE,
        emphasizedMode = EMPHASIZED_MODE_BOLD,
        newLinesAfter = 2
    ),
    ImagePrintable(
        bitmap = QRCode.from("ORDER#12345").bitmap(),
        alignment = ALIGNMENT_CENTER
    ),
    RawPrintable(
        data = byteArrayOf(27, 100, 4)  // ESC d 4 (feed paper)
    )
]
```

### Output ke Printer:
```
         TOKO SAYA          ← Large, Bold, Center
         


         [QR CODE]          ← QR Code 200x200
         


[Paper feeds 4 lines]       ← Raw ESC/POS command
```

---

## ⚡ Error Handling Flow

```
Print Request
    │
    ├─ Printer not paired?
    │  └─> Return: { ok: false, error: "Printer belum dipasangkan" }
    │
    ├─ JSON parse error?
    │  └─> Return: { ok: false, error: "Invalid JSON" }
    │
    ├─ Empty items?
    │  └─> Return: { ok: false, error: "No printable items found" }
    │
    ├─ Printer not initialized?
    │  └─> Return: { ok: false, error: "Printer not initialized" }
    │
    ├─ Connection failed?
    │  └─> Callback: connectionFailed(error)
    │      └─> Return: { ok: false, error: "Connection failed: ..." }
    │
    ├─ Print error?
    │  └─> Callback: onError(error)
    │      └─> Return: { ok: false, error: "Print error: ..." }
    │
    └─ Success?
       └─> Callback: printingOrderSentSuccessfully()
           └─> Return: { ok: true }
```

---

## 🔐 Security

### Trusted Page Check
```kotlin
private fun isTrustedPageMainThread(): Boolean {
    val url = webView.url ?: return false
    val host = url.toUri().host ?: return false
    return host == allowedHost  // e.g., "192.168.1.100:3000"
}
```

**Hanya request dari domain yang dipercaya yang akan diproses.**

---

## 📱 Cara Pairing Printer

```
1. Settings Android
      │
      ▼
2. Bluetooth
      │
      ▼
3. Pair New Device
      │
      ▼
4. Pilih Thermal Printer (e.g., "RPP02N", "BlueTooth Printer")
      │
      ▼
5. Paired Successfully
      │
      ▼
6. PrintoothBridge.hasPairedPrinter() → true
      │
      ▼
7. Siap print dari WebView
```

---

## 🎯 Keuntungan Arsitektur Ini

✅ **Separation of Concerns**
- Next.js: UI/UX logic
- Android Bridge: Communication layer
- PrintoothBridge: Printer logic

✅ **Type Safety**
- TypeScript di frontend
- Kotlin di backend
- JSON sebagai contract

✅ **Async/Await**
- Promise-based API
- Kotlin Coroutines
- Non-blocking operations

✅ **Error Handling**
- Try-catch di semua layer
- Descriptive error messages
- Graceful degradation

✅ **Maintainability**
- Single responsibility per class
- Easy to test
- Easy to extend

---

## 📊 Performance

| Operation | Time | Notes |
|-----------|------|-------|
| Init Printooth | ~100ms | Saat app start |
| Check paired | ~10ms | Local check |
| Parse template | ~50ms | Per 10 items |
| Print simple text | 2-3s | Depends on printer |
| Print with QR | 3-5s | QR generation + print |
| Print receipt | 5-10s | Multiple items |

---

## 🎓 Summary

**Alur Singkat:**
1. User di Next.js panggil `btPrintWithTemplate()`
2. Bridge kirim data ke Android via JavaScript Interface
3. AndroidBridge terima dan validasi
4. PrintoothBridge parse JSON jadi Printable objects
5. Printooth library kirim ke printer via Bluetooth
6. Callback sukses/gagal
7. Bridge kirim response balik ke WebView
8. Promise resolved di Next.js
9. User dapat result

**File Penting:**
- 📄 `android-printer-bridge.ts` - API untuk Next.js
- 📄 `AndroidBridge.kt` - Bridge layer
- 📄 `PrintoothBridge.kt` - Printer logic
- 📄 `MainActivity.kt` - Init bridge

**Sudah Siap Digunakan!** 🚀

