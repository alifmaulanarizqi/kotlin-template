# ✅ Feature: Auto Pairing Printer Bluetooth

## 🎯 Overview
Fitur ini membuat user experience lebih baik - ketika user mau print tapi printer belum paired, **app akan otomatis membuka activity untuk scanning dan pairing** Bluetooth printer, tanpa perlu manual ke Settings.

---

## 🔄 Alur Kerja

### **Before (Old Flow):**
```
User: Print
  ↓
App: ❌ Error "Printer belum dipasangkan"
  ↓
User: Manual ke Settings > Bluetooth > Pair
  ↓
User: Kembali ke app
  ↓
User: Print lagi
  ↓
App: ✅ Print berhasil
```

### **After (New Flow):**
```
User: Print
  ↓
App: Check paired printer
  ↓
  ├─ Sudah paired → ✅ Print langsung
  │
  └─ Belum paired → 🔍 Auto buka Pairing Activity
      ↓
      User: Pilih printer dari list
      ↓
      App: Pair otomatis
      ↓
      App: ✅ Lanjutkan print otomatis (resume)
```

---

## 📦 File yang Dibuat/Dimodifikasi

### 1. **PrinterPairingActivity.kt** ✅ NEW
**Path:** `app/src/main/java/com/example/kotlintemplate/ui/feature/printer/`

**Fungsi:**
- Activity Compose untuk pairing printer
- Launch Printooth ScanningActivity
- Handle hasil pairing (success/cancel)
- Auto-request Bluetooth permissions jika belum granted

**UI:**
```
┌─────────────────────────────┐
│ ← Back  Pair Bluetooth     │
│           Printer           │
├─────────────────────────────┤
│                             │
│          🖨️                  │
│                             │
│  Printer Bluetooth Belum    │
│      Terpasang              │
│                             │
│  Silakan scan dan pair      │
│  dengan printer thermal     │
│  Bluetooth Anda             │
│                             │
│  ┌───────────────────────┐  │
│  │ 🔍 Scan Printer       │  │
│  │    Bluetooth          │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │      Batal            │  │
│  └───────────────────────┘  │
│                             │
└─────────────────────────────┘
```

### 2. **PrinterPairingCoordinator.kt** ✅ NEW
**Path:** `app/src/main/java/com/example/kotlintemplate/ui/feature/printer/`

**Fungsi:**
- Coordinator pattern (seperti ScanCoordinator)
- Bridge antara AndroidBridge dan MainActivity
- Store callbacks untuk launch activity dan kirim result

```kotlin
object PrinterPairingCoordinator {
    var startPrinterPairing: ((requestId: String) -> Unit)? = null
    var sendPairingResult: ((requestId, success, printerName, printerMac) -> Unit)? = null
}
```

### 3. **MainActivity.kt** ✅ MODIFIED
**Perubahan:**
- Tambah `printerPairingLauncher` untuk handle pairing activity result
- Setup `PrinterPairingCoordinator` di `onCreate()`
- Handle pairing result dan kirim ke AndroidBridge

**Code snippet:**
```kotlin
private val printerPairingLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val requestId = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_REQUEST_ID).orEmpty()
        val success = result.data?.getBooleanExtra(PrinterPairingActivity.EXTRA_PAIRED_SUCCESS, false) ?: false
        val printerName = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_PRINTER_NAME)
        val printerMac = result.data?.getStringExtra(PrinterPairingActivity.EXTRA_PRINTER_MAC)

        PrinterPairingCoordinator.sendPairingResult?.invoke(requestId, success, printerName, printerMac)
    }

override fun onCreate(savedInstanceState: Bundle?) {
    // ...
    PrinterPairingCoordinator.startPrinterPairing = { requestId ->
        val intent = Intent(this, PrinterPairingActivity::class.java)
            .putExtra(PrinterPairingActivity.EXTRA_REQUEST_ID, requestId)
        printerPairingLauncher.launch(intent)
    }
}
```

### 4. **AndroidBridge.kt** ✅ MODIFIED
**Perubahan:**
- Tambah `pendingPrintRequests` map untuk menyimpan print request yang pending (saat pairing)
- Update `btPrintWithTemplate()` untuk auto-launch pairing jika belum paired
- Tambah `executePrint()` method (extracted untuk reusability)
- Tambah `handlePairingResult()` untuk handle hasil pairing dan resume print

**Logic Flow:**
```kotlin
btPrintWithTemplate(requestId, templateJson) {
    if (!hasPairedPrinter()) {
        // Save pending request
        pendingPrintRequests[requestId] = templateJson
        
        // Launch pairing activity
        PrinterPairingCoordinator.startPrinterPairing(requestId)
        return
    }
    
    // Execute print directly
    executePrint(requestId, templateJson)
}

handlePairingResult(requestId, success, ...) {
    if (success) {
        // Get pending request
        val templateJson = pendingPrintRequests.remove(requestId)
        
        // Resume print
        executePrint(requestId, templateJson)
    } else {
        // Pairing cancelled
        sendError("Pairing dibatalkan")
    }
}
```

### 5. **AndroidManifest.xml** ✅ MODIFIED
**Perubahan:**
- Register `PrinterPairingActivity`

```xml
<activity
    android:name=".ui.feature.printer.PrinterPairingActivity"
    android:exported="false"
    android:theme="@style/Theme.KotlinTemplate" />
```

---

## 🎨 Sequence Diagram

```
┌─────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────┐
│ Next.js │    │AndroidBridge │    │  MainActivity│    │ PairingActivity│  │Printooth│
└────┬────┘    └──────┬───────┘    └──────┬──────┘    └──────┬───────┘    └────┬────┘
     │                │                   │                   │                 │
     │ btPrintWith    │                   │                   │                 │
     │ Template()     │                   │                   │                 │
     ├───────────────>│                   │                   │                 │
     │                │                   │                   │                 │
     │                │ Check paired?     │                   │                 │
     │                ├──────────────────────────────────────────────────────> │
     │                │ FALSE             │                   │                 │
     │                │<────────────────────────────────────────────────────── │
     │                │                   │                   │                 │
     │                │ Save pending      │                   │                 │
     │                │ request           │                   │                 │
     │                │                   │                   │                 │
     │                │ Start pairing     │                   │                 │
     │                ├──────────────────>│                   │                 │
     │                │                   │                   │                 │
     │                │                   │ Launch activity   │                 │
     │                │                   ├──────────────────>│                 │
     │                │                   │                   │                 │
     │                │                   │                   │ Launch          │
     │                │                   │                   │ Scanning        │
     │                │                   │                   ├────────────────>│
     │                │                   │                   │                 │
     │                │                   │                   │  [User pilih    │
     │                │                   │                   │   printer]      │
     │                │                   │                   │                 │
     │                │                   │                   │ Pairing success │
     │                │                   │                   │<────────────────│
     │                │                   │                   │                 │
     │                │                   │ Result: success   │                 │
     │                │                   │<──────────────────│                 │
     │                │                   │                   │                 │
     │                │ Handle result     │                   │                 │
     │                │<──────────────────│                   │                 │
     │                │                   │                   │                 │
     │                │ Get pending req   │                   │                 │
     │                │ Resume print      │                   │                 │
     │                ├──────────────────────────────────────────────────────> │
     │                │                   │                   │                 │
     │                │ Print success     │                   │                 │
     │                │<────────────────────────────────────────────────────── │
     │                │                   │                   │                 │
     │ Result: OK     │                   │                   │                 │
     │<───────────────│                   │                   │                 │
     │                │                   │                   │                 │
```

---

## 🔧 Cara Kerja Detail

### Step 1: User Print (Printer Belum Paired)
```typescript
// Di Next.js
const result = await btPrintWithTemplate({
  items: [{ type: "text", text: "Hello" }]
});
```

### Step 2: AndroidBridge Check Pairing
```kotlin
// Di AndroidBridge.kt
fun btPrintWithTemplate(requestId: String, templateJson: String) {
    val hasPaired = PrintoothBridge.hasPairedPrinter()
    
    if (!hasPaired) {
        // Save untuk resume nanti
        pendingPrintRequests[requestId] = templateJson
        
        // Launch pairing
        PrinterPairingCoordinator.startPrinterPairing(requestId)
        return
    }
    
    // ... print directly
}
```

### Step 3: MainActivity Launch Activity
```kotlin
// Di MainActivity.kt
PrinterPairingCoordinator.startPrinterPairing = { requestId ->
    val intent = Intent(this, PrinterPairingActivity::class.java)
        .putExtra(PrinterPairingActivity.EXTRA_REQUEST_ID, requestId)
    printerPairingLauncher.launch(intent)
}
```

### Step 4: PrinterPairingActivity Scan
```kotlin
// Di PrinterPairingActivity.kt
private fun startPrintoothScanning() {
    scanningLauncher.launch(
        Intent(this, ScanningActivity::class.java)
    )
}
```

Printooth ScanningActivity akan:
1. Scan Bluetooth devices
2. Filter hanya printer devices
3. Tampilkan list
4. User pilih printer
5. Auto-pair
6. Return result

### Step 5: Handle Result & Resume Print
```kotlin
// Di MainActivity.kt → AndroidBridge.kt
printerPairingLauncher result → 
    PrinterPairingCoordinator.sendPairingResult(requestId, success, ...) → 
        AndroidBridge.handlePairingResult(requestId, success, ...)

// Di AndroidBridge.kt
private fun handlePairingResult(...) {
    if (success) {
        val templateJson = pendingPrintRequests.remove(requestId)
        executePrint(requestId, templateJson) // RESUME PRINT!
    } else {
        sendError("Pairing dibatalkan")
    }
}
```

---

## 📱 User Experience

### Scenario 1: Printer Sudah Paired
```
User: Klik Print
  ↓
App: ✅ Langsung print (0.5 detik)
```

### Scenario 2: Printer Belum Paired
```
User: Klik Print
  ↓
App: Opening pairing screen... (0.3 detik)
  ↓
Screen: "Printer Bluetooth Belum Terpasang"
        [🔍 Scan Printer Bluetooth] [Batal]
  ↓
User: Klik "Scan Printer Bluetooth"
  ↓
App: Request Bluetooth permission (jika belum granted)
  ↓
User: Allow permission
  ↓
App: Scanning... (2-5 detik)
  ↓
Screen: List printer yang ditemukan
        • BlueTooth Printer (11:22:33:44:55:66)
        • RPP02N (AA:BB:CC:DD:EE:FF)
  ↓
User: Pilih printer
  ↓
App: Pairing... (1-2 detik)
  ↓
App: ✅ Paired! Resuming print... (0.5 detik)
  ↓
App: ✅ Print berhasil!
```

### Scenario 3: User Cancel Pairing
```
User: Klik Print
  ↓
App: Opening pairing screen...
  ↓
User: Klik "Batal"
  ↓
App: ❌ Error "Pairing dibatalkan. Printer belum terpasang."
  ↓
(No print executed)
```

---

## 🎯 Benefits

### 1. **Better UX** ✅
- Tidak perlu manual ke Settings
- Flow lebih smooth dan intuitive
- Auto-resume print setelah pairing

### 2. **Less Friction** ✅
- User tidak perlu tahu cara pair Bluetooth
- App guide user step-by-step
- Integrated flow dalam app

### 3. **Error Recovery** ✅
- Jika pairing gagal, user bisa retry
- Clear error messages
- Cancel option tersedia

### 4. **Consistency** ✅
- Sama seperti QR Scanner (ScanCoordinator pattern)
- Consistent architecture
- Easy to maintain

---

## 🔐 Permissions

Activity akan auto-request permissions yang diperlukan:

### Android 12+ (API 31+)
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

### Android 11 and below
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

---

## 📊 Testing

### Test 1: Print dengan Printer Belum Paired
```typescript
const result = await btPrintWithTemplate({
  items: [{ type: "text", text: "Test" }]
});

// Expected: Pairing activity muncul
```

### Test 2: Cancel Pairing
```typescript
const result = await btPrintWithTemplate({...});

// User klik "Batal"

// Expected: { ok: false, error: "Pairing dibatalkan..." }
```

### Test 3: Successful Pairing & Print
```typescript
const result = await btPrintWithTemplate({...});

// User pilih printer → pair → auto print

// Expected: { ok: true }
```

### Test 4: Print dengan Printer Sudah Paired
```typescript
// Printer sudah paired sebelumnya
const result = await btPrintWithTemplate({...});

// Expected: Langsung print, no pairing screen
```

---

## 🐛 Troubleshooting

### Issue: "Permission denied"
**Solusi:** App akan auto-request, tapi pastikan user klik "Allow"

### Issue: "No devices found"
**Solusi:** 
- Pastikan printer ON dan dalam pairing mode
- Pastikan Bluetooth HP ON
- Coba scan lagi

### Issue: "Pairing failed"
**Solusi:**
- Restart printer
- Unpair dari Settings jika sudah paired
- Coba pair lagi

---

## ✅ Status: **IMPLEMENTED & READY TO TEST**

### Files Created:
1. ✅ `PrinterPairingActivity.kt`
2. ✅ `PrinterPairingCoordinator.kt`

### Files Modified:
1. ✅ `MainActivity.kt`
2. ✅ `AndroidBridge.kt`
3. ✅ `AndroidManifest.xml`

### Next Steps:
1. Build & install app
2. Test print tanpa printer paired
3. Verify pairing flow works
4. Test auto-resume print after pairing

**Happy Testing!** 🚀

