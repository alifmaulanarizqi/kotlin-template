# Panduan Penggunaan Printooth Bridge untuk Thermal Printer

## Overview
Project ini sudah diintegrasikan dengan **Printooth Library** untuk print ke thermal printer Bluetooth. Ada 2 cara print yang tersedia:

1. **Manual ESCPOS** - Kirim raw bytes ESCPOS (cara lama, lebih kompleks)
2. **Printooth Template** - Kirim template JSON (cara baru, lebih mudah) ✅ **RECOMMENDED**

## Persiapan

### 1. Pair Printer via Bluetooth Settings
Sebelum bisa print, printer **HARUS** sudah dipasangkan melalui Settings Android:
```
Settings > Bluetooth > Pair New Device > Pilih printer thermal Anda
```

### 2. Copy File ke Next.js
Copy file `android-printer-bridge.ts` ke project Next.js Anda.

## Cara Penggunaan

### A. Cara Mudah - Printooth Template (RECOMMENDED)

#### 1. Check apakah printer sudah paired
```typescript
import { btHasPairedPrinter } from './android-printer-bridge';

const result = await btHasPairedPrinter();
if (!result.ok || !result.hasPaired) {
  alert('Silakan pair printer di Settings > Bluetooth');
  return;
}
```

#### 2. Print Simple Text
```typescript
import { printSimpleText } from './android-printer-bridge';

const result = await printSimpleText("Hello World!");
if (result.ok) {
  console.log('✅ Print berhasil');
} else {
  console.error('❌ Print gagal:', result.error);
}
```

#### 3. Print Receipt Lengkap
```typescript
import { printReceipt } from './android-printer-bridge';

const result = await printReceipt({
  storeName: "TOKO SAYA",
  storeAddress: "Jl. Contoh No. 123, Jakarta",
  items: [
    { name: "Kopi", qty: 2, price: 15000, total: 30000 },
    { name: "Teh", qty: 1, price: 10000, total: 10000 },
  ],
  total: 40000
});
```

#### 4. Print dengan QR Code
```typescript
import { printWithQR } from './android-printer-bridge';

const result = await printWithQR(
  "Scan untuk info lebih lanjut",
  "https://example.com/order/12345"
);
```

#### 5. Print Custom Template
```typescript
import { btPrintWithTemplate } from './android-printer-bridge';

const result = await btPrintWithTemplate({
  items: [
    // Text biasa
    {
      type: "text",
      text: "Welcome",
      alignment: "center",
      fontSize: "large",
      bold: true,
      newLinesAfter: 2
    },
    
    // Text dengan formatting
    {
      type: "text",
      text: "TID: 1111123322",
      alignment: "left",
      newLinesAfter: 1
    },
    
    // QR Code
    {
      type: "qr",
      data: "Order #12345",
      size: 200,
      alignment: "center"
    },
    
    // Separator
    {
      type: "text",
      text: "--------------------------------",
      newLinesAfter: 1
    },
    
    // Total dengan bold
    {
      type: "text",
      text: "TOTAL: Rp 200,000",
      bold: true,
      fontSize: "large",
      alignment: "center",
      newLinesAfter: 2
    },
    
    // Raw ESC/POS command (feed paper)
    {
      type: "raw",
      bytes: [27, 100, 4]  // ESC d 4 (feed 4 lines)
    }
  ]
});
```

### Template Item Types

#### Text Item
```typescript
{
  type: "text",
  text: "Your text here",
  alignment?: "left" | "center" | "right",  // default: "left"
  fontSize?: "normal" | "large" | "wide" | "tall" | "big",  // default: "normal"
  bold?: boolean,  // default: false
  underline?: boolean,  // default: false
  newLinesAfter?: number,  // default: 0
  lineSpacing?: number,  // 30 atau 60
  charCode?: number  // character encoding
}
```

#### QR Code Item
```typescript
{
  type: "qr",
  data: "Text to encode in QR",
  size?: number,  // default: 200 (pixels)
  alignment?: "left" | "center" | "right"  // default: "center"
}
```

#### Raw ESC/POS Item
```typescript
{
  type: "raw",
  bytes: [27, 100, 4]  // ESC/POS command bytes
}
```

### Common ESC/POS Commands (untuk type: "raw")

```typescript
// Initialize printer
[27, 64]  // ESC @

// Feed paper
[27, 100, 4]  // ESC d 4 (feed 4 lines)
[10]  // LF (line feed 1 line)

// Cut paper (jika printer support)
[27, 109]  // ESC m (partial cut)
[27, 105]  // ESC i (full cut)

// Open cash drawer (jika ada)
[27, 112, 0, 25, 250]  // ESC p 0 25 250
```

---

### B. Cara Manual - ESCPOS (untuk advanced users)

Jika ingin kontrol penuh, bisa pakai manual ESCPOS:

```typescript
import { btListPairedPrinters, btConnectPrinter, btPrintToPrinter, btDisconnectPrinter } from './android-printer-bridge';

// 1. List paired devices
const devices = await btListPairedPrinters();
console.log(devices.devices); // [{ name: "Printer", mac: "00:11:22:33:44:55" }]

// 2. Connect
const mac = devices.devices[0].mac;
await btConnectPrinter(mac);

// 3. Prepare ESC/POS commands
const commands = [
  0x1B, 0x40,  // Initialize
  0x48, 0x45, 0x4C, 0x4C, 0x4F,  // "HELLO"
  0x0A, 0x0A, 0x0A, 0x0A, 0x0A  // Line feeds
];

// 4. Convert to base64
const base64 = btoa(String.fromCharCode.apply(null, commands));

// 5. Print
await btPrintToPrinter(mac, base64);

// 6. Wait & disconnect
await new Promise(resolve => setTimeout(resolve, 2000));
await btDisconnectPrinter(mac);
```

---

## Troubleshooting

### ❌ "Printer belum dipasangkan"
**Solusi:** Pair printer di Settings > Bluetooth terlebih dahulu

### ❌ "Connection failed"
**Solusi:** 
- Pastikan printer dalam jangkauan
- Restart printer
- Unpair dan pair ulang di Bluetooth Settings

### ❌ "Print gagal" tapi tidak ada error
**Solusi:**
- Pastikan printer memiliki kertas
- Cek apakah printer dalam kondisi siap (LED hijau/biru)
- Coba print test dari aplikasi printer lain untuk validasi

### ❌ Printer print tapi kertas tidak keluar
**Solusi:** Tambahkan raw command untuk feed paper:
```typescript
{
  type: "raw",
  bytes: [27, 100, 4]  // Feed 4 lines
}
```

---

## Contoh Lengkap di Next.js Component

```typescript
'use client';

import { useState } from 'react';
import { btHasPairedPrinter, printReceipt } from '@/lib/android-printer-bridge';

export default function PrinterDemo() {
  const [status, setStatus] = useState('');

  const handlePrint = async () => {
    try {
      setStatus('Checking printer...');
      
      // Check if paired
      const checkResult = await btHasPairedPrinter();
      if (!checkResult.ok || !checkResult.hasPaired) {
        setStatus('❌ Printer belum dipasangkan. Silakan pair di Settings > Bluetooth');
        return;
      }

      setStatus('Printing...');

      // Print receipt
      const result = await printReceipt({
        storeName: "CAFE KOPI",
        storeAddress: "Jl. Sudirman No. 123",
        items: [
          { name: "Americano", qty: 2, price: 25000, total: 50000 },
          { name: "Latte", qty: 1, price: 30000, total: 30000 },
        ],
        total: 80000
      });

      if (result.ok) {
        setStatus('✅ Print berhasil!');
      } else {
        setStatus(`❌ Print gagal: ${result.error}`);
      }
    } catch (error) {
      setStatus(`❌ Error: ${error.message}`);
    }
  };

  return (
    <div className="p-4">
      <button 
        onClick={handlePrint}
        className="bg-blue-500 text-white px-4 py-2 rounded"
      >
        Print Receipt
      </button>
      
      {status && (
        <div className="mt-4 p-4 bg-gray-100 rounded">
          {status}
        </div>
      )}
    </div>
  );
}
```

---

## Keuntungan Printooth Template vs Manual ESCPOS

| Fitur | Printooth Template | Manual ESCPOS |
|-------|-------------------|---------------|
| **Kemudahan** | ✅ Sangat mudah | ❌ Kompleks |
| **QR Code** | ✅ Built-in | ❌ Harus generate manual |
| **Formatting** | ✅ Simple JSON | ❌ Harus tau byte codes |
| **Error Handling** | ✅ Auto handle | ❌ Manual |
| **Connection** | ✅ Auto connect | ❌ Manual connect/disconnect |
| **Maintenance** | ✅ Mudah | ❌ Sulit debug |

---

## File-file yang Terlibat

### Kotlin (Android)
1. **PrintoothBridge.kt** - Wrapper Printooth library
2. **AndroidBridge.kt** - Bridge antara WebView & Native
3. **BridgeEntryPoint.kt** - Hilt dependency injection

### TypeScript (Next.js)
1. **android-printer-bridge.ts** - Bridge helper untuk Next.js
2. **printer-helper.js** - ESC/POS helper (cara manual/lama)

---

## Support

Jika ada masalah, pastikan:
1. ✅ Printer sudah paired di Bluetooth Settings
2. ✅ Printer dalam kondisi ON dan siap
3. ✅ App memiliki permission Bluetooth (sudah auto request)
4. ✅ Running di Android WebView (tidak bisa di browser biasa)

---

**Happy Printing! 🖨️**

