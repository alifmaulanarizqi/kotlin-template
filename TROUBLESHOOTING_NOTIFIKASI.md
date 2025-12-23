# 🔧 TROUBLESHOOTING - Notifikasi Tidak Muncul

## ✅ Perbaikan yang Sudah Dilakukan

### Masalah Ditemukan
1. **Waktu Jadwal Terlalu Jauh** - Dijadwalkan untuk jam 15:00 tetapi sekarang mungkin sudah lewat
2. **Tidak Ada Test Immediate** - Sulit verify apakah sistem notifikasi bekerja
3. **Permission Check Tidak Verbose** - Sulit debug di mana fail

### Solusi yang Diterapkan

#### 1. **Ubah Waktu Jadwal**
- ~~Sebelum: Jam 15:00 (tetap/fixed)~~
- ✅ Sesudah: 2 menit dari sekarang (dynamic)
- Sekarang mudah test tanpa menunggu lama

#### 2. **Tambah NotificationHelper** 
- Fungsi `showNotificationImmediately()` untuk test langsung
- Tidak perlu menunggu alarm trigger
- Langsung terlihat apakah notifikasi bekerja

#### 3. **Tambah Logging Verbose**
- Debug-friendly logs di setiap tahap
- Mudah lihat di logcat apakah permission grant, schedule, dll

## 🚀 CARA TEST SEKARANG

### Step 1: Install Aplikasi
```bash
.\gradlew clean installDebug
```

### Step 2: Lihat Logcat Saat App Dibuka
```bash
adb logcat -c
adb logcat | findstr MainActivity
```

### Step 3: Berikan Permission
- Aplikasi akan minta permission "Post notifications"
- **PENTING**: Ketika diberi permission, notifikasi **langsung muncul** di tray

### Expected Output
```
D/MainActivity: Android 13+: Checking POST_NOTIFICATIONS permission
D/MainActivity: Permission not granted, requesting...
[User taps Allow]
D/NotificationHelper: ✅ Notifikasi berhasil ditampilkan: Permission Granted ✅
D/MainActivity: POST_NOTIFICATIONS permission granted
D/MainActivity: Notifikasi dijadwalkan untuk Tue Dec 24 14:32:00 GMT+07:00 2025
```

### Step 4: Tunggu 2 Menit (untuk notifikasi terjadwal)
- Setelah 2 menit, notifikasi lain akan muncul
- Judul: "Notifikasi Terjadwal"
- Text: "Notifikasi akan muncul dalam 2 menit"

## 🔍 CEK APAKAH NOTIFIKASI BEKERJA

### Test 1: Notifikasi Langsung (Immediate)
**Harus muncul langsung saat permission granted**
- Buka aplikasi
- Ketika permission dialog muncul, tap **Allow**
- Notifikasi langsung muncul dengan text "Permission Granted ✅"
- **Jika muncul**: ✅ System notifikasi bekerja!
- **Jika tidak**: Lihat "TROUBLESHOOTING CHECKLIST" di bawah

### Test 2: Notifikasi Terjadwal (Alarm-based)
**Harus muncul setelah 2 menit**
- Lihat jam sekarang
- Tunggu 2 menit
- Notifikasi kedua muncul dengan judul "Notifikasi Terjadwal"
- **Jika muncul**: ✅ AlarmManager bekerja!
- **Jika tidak**: Lihat "TROUBLESHOOTING CHECKLIST" di bawah

## ⚠️ TROUBLESHOOTING CHECKLIST

### Jika Notifikasi Langsung Tidak Muncul

#### ❌ Problem 1: Permission Dialog Tidak Muncul
```
Solusi:
1. Pastikan Android 13+ (TIRAMISU)
2. Jika Android 12 atau lebih lama, notifikasi harus langsung muncul
   (tidak ada permission dialog)
3. Lihat logcat: "Android 13+: Checking POST_NOTIFICATIONS permission"
```

#### ❌ Problem 2: Permission Dialog Muncul tapi Tidak Bisa Tap "Allow"
```
Solusi:
1. Dialog mungkin custom di aplikasi ini
2. Cari tombol permission grant di UI
3. Atau lihat Android Settings > Apps > [app] > Permissions > Notifications
```

#### ❌ Problem 3: Permission Granted Tapi Notifikasi Tidak Muncul
```
Solusi:
1. CEK BATTERY SETTINGS:
   - Settings > Battery > Battery Saver/Power Saving Mode
   - Matikan untuk app ini
   
2. CEK NOTIFICATION SETTINGS:
   - Settings > Apps > [app] > Permissions > Notifications
   - Pastikan Toggle "ON" / "Allowed"
   
3. CEK APP NOTIFICATION SETTINGS:
   - Settings > Apps > Notifications > [app]
   - Pastikan tidak muted, importance HIGH
   
4. CEK DEVICE NOTIFICATIONS:
   - Settings > Sound & Vibration > Notifications
   - Pastikan tidak silent/muted
```

#### ❌ Problem 4: Logcat Error "Permission not granted"
```
Solusi:
1. Di Android 13+, permission mungkin belum diberikan
2. Buka Settings > Apps > [app] > Permissions
3. Pastikan "Post notifications" sudah Allow
4. Restart aplikasi
```

### Jika Notifikasi Terjadwal Tidak Muncul

#### ❌ Problem 1: Notifikasi Langsung Muncul tapi Terjadwal Tidak
```
Solusi:
1. AlarmManager mungkin tidak dijadwalkan dengan benar
2. Cek logcat: "Notifikasi dijadwalkan untuk [time]"
3. Jika tidak ada log, error ada di NotificationScheduler
4. Cek phone battery - mungkin device doze/sleep
```

#### ❌ Problem 2: Device Doze Mode Mematikan Alarm
```
Solusi:
1. Matikan Battery Saver:
   - Settings > Battery > Battery Saver Mode = OFF
   
2. Exclude app dari Doze:
   - Settings > Apps > [app] > Battery
   - Pilih "Unrestricted" or "No restriction"
   
3. Atau add app ke whitelist battery optimization
```

#### ❌ Problem 3: Alarm Trigger tapi Notifikasi Tidak Muncul
```
Solusi:
1. Lihat logcat saat alarm trigger (setelah 2 menit)
2. Cari: "D/NotificationReceiver: Notifikasi ditampilkan"
3. Jika ada ERROR, cek log untuk exception
4. Mungkin permission belum diberikan saat alarm trigger
```

## 📊 DIAGNOSTIC LOG

### Buat file dengan output logcat lengkap:
```bash
adb logcat > logcat.txt 2>&1
# (buka app, tap allow, tunggu 2 menit)
# Ctrl+C setelah selesai
```

### Cari baris penting:
```bash
grep -E "MainActivity|NotificationReceiver|NotificationScheduler|NotificationHelper" logcat.txt
```

### Expected baris (dalam urutan):
```
1. "Android 13+: Checking POST_NOTIFICATIONS permission"
2. "Permission not granted, requesting..."
3. "POST_NOTIFICATIONS permission granted"
4. "Attempting to show notification: Permission Granted ✅"
5. "✅ Notifikasi berhasil ditampilkan: Permission Granted ✅"
6. "Notifikasi dijadwalkan untuk [time in 2 minutes]"
7. (after 2 minutes) "Notifikasi ditampilkan: Notifikasi Terjadwal"
```

## 🎯 QUICK DIAGNOSTIC

Jalankan perintah ini dan bagikan outputnya jika ada masalah:

```bash
# 1. Clear logcat
adb logcat -c

# 2. Start app
adb shell am start -n com.example.kotlintemplate/.MainActivity

# 3. Capture logs (15 detik)
timeout 15 adb logcat 2>&1 | findstr /E "MainActivity|NotificationReceiver|NotificationScheduler|NotificationHelper|permission" > diagnostic.txt

# 4. Show hasil
type diagnostic.txt
```

## ✨ BEST PRACTICES

### Kalau Ingin Notifikasi Muncul di Jam Tertentu

```kotlin
// Untuk jam pasti (misalnya 15:00)
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 15,
    minute = 0,
    title = "Daily Meeting",
    message = "Meeting dimulai"
)

// Untuk offset dari sekarang
val targetTime = java.util.Calendar.getInstance()
targetTime.add(java.util.Calendar.MINUTE, 5) // 5 menit dari sekarang
NotificationScheduler.scheduleNotification(
    context = this,
    hour = targetTime.get(java.util.Calendar.HOUR_OF_DAY),
    minute = targetTime.get(java.util.Calendar.MINUTE),
    title = "Reminder",
    message = "Check your tasks"
)
```

### Test Notifikasi Langsung Kapan Saja

```kotlin
import com.example.kotlintemplate.notification.NotificationHelper

// Di Activity, Service, Fragment, dll
NotificationHelper.showNotificationImmediately(
    context = this,
    title = "Test",
    message = "Ini notifikasi langsung tanpa alarm"
)
```

---

## 🎓 SUMMARY

| Scenario | Expected | Jika Tidak Muncul |
|----------|----------|------------------|
| **App Dibuka (Android 13+)** | Permission dialog muncul | Cek Android version, cek logcat |
| **Permission Granted** | Notifikasi langsung muncul | Cek Settings > Notifications |
| **Setelah 2 Menit** | Notifikasi terjadwal muncul | Cek Device Doze, cek battery settings |

---

**Jika masih ada masalah**, jalankan diagnostic commands di atas dan bagikan outputnya!

