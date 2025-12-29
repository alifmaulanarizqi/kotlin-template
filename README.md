# 🖨️ Android WebView App with Bluetooth Printer Support

Template aplikasi Android dengan WebView yang mendukung **Bluetooth Thermal Printer** menggunakan Printooth library.

## ✨ Features

### 🎯 Core Features
- ✅ **WebView dengan Android Bridge** - Komunikasi 2-arah antara JavaScript dan Native Android
- ✅ **QR Code Scanner** - Scan QR code dari WebView dengan Camera X & ML Kit
- ✅ **Bluetooth Thermal Printer** - Print receipt ke printer thermal via Bluetooth
- ✅ **Auto Printer Pairing** - Otomatis membuka pairing screen jika printer belum paired
- ✅ **Template-based Printing** - Print dengan JSON template (text, QR code, raw ESC/POS)
- ✅ **Offline Storage** - Room database untuk data lokal
- ✅ **Background Sync** - WorkManager untuk sync data ke API

### 🏗️ Architecture
- **MVVM** dengan Clean Architecture
- **Jetpack Compose** untuk UI
- **Hilt** untuk Dependency Injection
- **Kotlin Coroutines** & Flow
- **Retrofit** untuk networking
- **Room** untuk local database

## 📦 Struktur Project

```
app/
├── src/main/java/com/example/kotlintemplate/
│   ├── MyApp.kt                          # Application class
│   ├── MainActivity.kt                   # Main activity
│   │
│   ├── core/
│   │   └── common/
│   │       ├── AndroidBridge.kt          # WebView ↔ Native bridge
│   │       └── PrintoothBridge.kt        # Wrapper Printooth library
│   │
│   ├── ui/
│   │   ├── feature/
│   │   │   ├── scan/
│   │   │   │   ├── QrScanActivity.kt     # QR scanner activity
│   │   │   │   └── ScanCoordinator.kt    # Coordinator pattern
│   │   │   ├── printer/
│   │   │   │   ├── PrinterPairingActivity.kt  # Printer pairing UI
│   │   │   │   └── PrinterPairingCoordinator.kt
│   │   │   └── webview/
│   │   │       └── WebScreen.kt          # WebView composable
│   │   │
│   │   ├── navigation/
│   │   └── theme/
│   │
│   ├── data/
│   │   ├── local/                        # Room database
│   │   ├── remote/                       # Retrofit API
│   │   └── repository/
│   │
│   └── domain/
│       ├── model/
│       └── usecase/
│
└── docs/
    ├── ALUR_KERJA_PRINTOOTH_BRIDGE.md   # Dokumentasi alur kerja printer
    ├── FEATURE_AUTO_PAIRING_PRINTER.md   # Dokumentasi fitur auto-pairing
    ├── PANDUAN_PRINTOOTH_BRIDGE.md       # Panduan penggunaan
    ├── NEXTJS_AUTO_PAIRING_GUIDE.tsx     # Guide untuk Next.js developer
    └── android-printer-bridge.ts         # TypeScript bridge helper
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK API 24+

### Installation

1. Clone repository
```bash
git clone <repository-url>
cd KotlinTemplate
```

2. Open di Android Studio

3. Sync Gradle dependencies

4. Build & Run
```bash
./gradlew assembleDevDebug
```

## 🖨️ Bluetooth Printer Integration

### Setup di Next.js

1. Copy file `android-printer-bridge.ts` ke project Next.js Anda

2. Import dan gunakan:

```typescript
import { btPrintWithTemplate } from './android-printer-bridge';

// Print receipt
const result = await btPrintWithTemplate({
  items: [
    {
      type: "text",
      text: "TOKO SAYA",
      alignment: "center",
      fontSize: "large",
      bold: true,
      newLinesAfter: 2
    },
    {
      type: "qr",
      data: "ORDER#12345",
      size: 200,
      alignment: "center"
    },
    {
      type: "raw",
      bytes: [27, 100, 4] // ESC/POS feed paper
    }
  ]
});

if (result.ok) {
  console.log('✅ Print berhasil!');
} else {
  console.error('❌ Error:', result.error);
}
```

### Template Items Support

| Type | Description | Parameters |
|------|-------------|------------|
| **text** | Print text dengan formatting | `text`, `alignment`, `fontSize`, `bold`, `underline`, `newLinesAfter` |
| **qr** | Print QR code | `data`, `size`, `alignment` |
| **raw** | Raw ESC/POS commands | `bytes` (array of numbers) |

### Auto Printer Pairing

Jika printer belum paired, app akan **otomatis membuka pairing screen**:
1. User klik Print di WebView
2. App check: printer paired?
3. Jika belum → Launch pairing activity
4. User pilih printer dari list
5. Auto-pair (2-3 detik)
6. Auto-resume print

**Tidak perlu manual ke Settings > Bluetooth!** ✨

## 📱 Permissions

### Bluetooth (Auto-requested)
```xml
<!-- Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Android 11 and below -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Camera (untuk QR Scanner)
```xml
<uses-permission android:name="android.permission.CAMERA"/>
```

### Internet
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 🔧 Configuration

### Build Flavors

App memiliki 3 build flavors:

- **dev** - Development (`.dev` suffix)
- **staging** - Staging (`.stg` suffix)
- **production** - Production (no suffix)

Build specific flavor:
```bash
./gradlew assembleDevDebug
./gradlew assembleStagingRelease
./gradlew assembleProductionRelease
```

### WebView URL

Update URL WebView di `WebScreen.kt`:
```kotlin
val webUrl = "https://your-nextjs-app.com"
```

Atau set di `build.gradle.kts`:
```kotlin
buildConfigField("String", "WEB_URL", "\"https://example.com\"")
```

## 📚 Documentation

### Untuk Developer Android
- **ALUR_KERJA_PRINTOOTH_BRIDGE.md** - Alur kerja lengkap sistem printer
- **FEATURE_AUTO_PAIRING_PRINTER.md** - Detail fitur auto-pairing
- **PANDUAN_PRINTOOTH_BRIDGE.md** - Panduan penggunaan Printooth

### Untuk Developer Next.js
- **NEXTJS_AUTO_PAIRING_GUIDE.tsx** - Quick guide dengan code examples
- **android-printer-bridge.ts** - TypeScript helper (copy ke Next.js project)

## 🧪 Testing

### Test Print
1. Pastikan printer thermal Bluetooth ON
2. Buka app
3. Di Next.js WebView, call:
```typescript
const result = await btPrintWithTemplate({
  items: [
    { type: "text", text: "Test Print", alignment: "center" }
  ]
});
```

### Test Auto-Pairing
1. Unpair printer dari Settings (jika sudah paired)
2. Call print function
3. Pairing screen akan muncul otomatis
4. Pilih printer → Auto-pair → Auto-print

## 📊 Libraries Used

### Core
- **Kotlin** 2.0.0
- **Jetpack Compose** - Modern Android UI
- **Hilt** - Dependency injection
- **Navigation Compose** - Navigation

### Networking
- **Retrofit** - REST API client
- **OkHttp** - HTTP client
- **Gson** - JSON serialization

### Database
- **Room** - Local database
- **WorkManager** - Background tasks

### Camera & ML
- **CameraX** - Camera API
- **ML Kit Barcode** - QR code scanning

### Printer
- **Printooth** 1.3.1 - Bluetooth thermal printer
- **QRGen** 2.6.0 - QR code generation

### Utilities
- **Timber** - Logging
- **Coil** - Image loading

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

- Your Name / Team Name

## 📞 Support

Untuk pertanyaan atau issue, silakan buka GitHub Issues.

---

**Happy Coding!** 🚀

