# Panduan Notifikasi Terjadwal

## Perbaikan Crash yang Dilakukan

Kode dari ChatGPT memiliki beberapa masalah yang menyebabkan crash:
1. **PendingIntent FLAG_MUTABLE** - Tidak aman dan bisa menyebabkan crash pada beberapa device
2. **Tidak ada error handling** - AlarmManager bisa melempar exception tanpa penanganan
3. **Akses NotificationManager langsung tanpa null-check** - Bisa NPE pada beberapa device
4. **Tidak ada permission check** - POST_NOTIFICATIONS tidak dicek sebelum notify()

## Solusi yang Diterapkan

### 1. NotificationScheduler (DIPERBAIKI)
- Menggunakan `FLAG_IMMUTABLE` (lebih aman)
- Membungkus AlarmManager calls dengan try/catch
- Fallback dari setExact ke setAndAllowWhileIdle jika diperlukan
- Menggunakan `applicationContext` (aman untuk semua context)

### 2. NotificationReceiver (DIPERBAIKI)
- Null-check untuk Intent
- Permission check untuk POST_NOTIFICATIONS
- Try/catch di setiap operasi
- Null-check untuk NotificationManager

### 3. MainActivity (DIPERBAIKI)
- Meminta permission POST_NOTIFICATIONS (Android 13+)
- Hanya schedule setelah permission granted

### 4. AndroidManifest.xml (DITAMBAHKAN)
- Menambahkan permission: `android.permission.SCHEDULE_EXACT_ALARM`

## Cara Menggunakan

### Setup Awal (Sudah Dilakukan)
Kode sudah terintegrasi di MainActivity, tinggal run aplikasi:

```bash
.\gradlew installDebug
```

### Manual Schedule Notifikasi dari Code Manapun

Gunakan `NotificationScheduler.scheduleNotification()`:

```kotlin
import com.example.kotlintemplate.notification.NotificationScheduler

// Jadwalkan notifikasi jam 15:00 hari ini
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 15,
    minute = 0,
    title = "Pengingat",
    message = "Ini adalah notifikasi terjadwal"
)

// Jadwalkan notifikasi jam 14:30
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 14,
    minute = 30,
    title = "Meeting",
    message = "Meeting dimulai dalam 30 menit"
)
```

### Dari Service/Worker

```kotlin
// Dari Service
class MyService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationScheduler.scheduleNotification(
            context = applicationContext,  // Gunakan applicationContext!
            hour = 10,
            minute = 0,
            title = "Reminder",
            message = "Check your tasks"
        )
        return START_STICKY
    }
}
```

### Dari ViewModel

```kotlin
class MyViewModel : ViewModel() {
    fun scheduleNotification(context: Context) {
        NotificationScheduler.scheduleNotification(
            context = context.applicationContext,
            hour = 16,
            minute = 45,
            title = "Task",
            message = "Don't forget your task"
        )
    }
}
```

## Testing

### Test di Device/Emulator

1. **Install & Buka Aplikasi**
   ```bash
   .\gradlew installDebug
   # Buka aplikasi di device
   ```

2. **Berikan Permission**
   - Aplikasi akan meminta izin POST_NOTIFICATIONS di Android 13+
   - Berikan izin (Allow)

3. **Tunggu Alarm Trigger**
   - Default sudah dijadwalkan jam 15:00
   - Atau ubah jam di MainActivity untuk test lebih cepat

4. **Lihat Notifikasi**
   - Notifikasi akan muncul di notification tray pada waktu yang dijadwalkan

### Debug Logcat

```bash
# Lihat log notifikasi
adb logcat | findstr NotificationReceiver
adb logcat | findstr NotificationScheduler

# Lihat semua log untuk package ini
adb logcat | findstr com.example.kotlintemplate
```

### Contoh Log yang Diharapkan

```
D/NotificationScheduler: Notifikasi dijadwalkan untuk Tue Dec 24 15:00:00 GMT+07:00 2025
D/NotificationReceiver: Notifikasi ditampilkan: Pengingat
```

## File yang Diubah

1. **NotificationScheduler.kt** - Completely rewritten dengan error handling
2. **NotificationReceiver.kt** - Completely rewritten dengan null-checks
3. **MainActivity.kt** - Updated untuk permission handling
4. **AndroidManifest.xml** - Added SCHEDULE_EXACT_ALARM permission

## Catatan Penting

### Waktu Jadwal
- Format: 24 jam (0-23 untuk jam)
- Jika waktu sudah lewat hari ini, otomatis dijadwalkan ke besok
- Contoh: jam 15:30 hari ini, jika sekarang 16:00, jadwal ke besok 15:30

### Permission
- Android 13+ memerlukan POST_NOTIFICATIONS permission
- Android 12+ memerlukan SCHEDULE_EXACT_ALARM untuk presisi
- Sudah ditambahkan ke AndroidManifest.xml

### Stability
- Menggunakan try/catch untuk semua operasi kritis
- Fallback ke non-exact alarm jika setExact gagal
- Logging lengkap untuk debugging

### Maintenance
- Alarm tidak bertahan setelah reboot device
- Untuk reboot persistence, perlu tambah BOOT_COMPLETED receiver (optional)
- Saat ini satu alarm per waktu (tidak recurring otomatis)

## Troubleshooting

### Notifikasi Tidak Muncul
1. Periksa izin: Settings → Apps → Permission → Notifications
2. Lihat logcat untuk error: `adb logcat | grep NotificationReceiver`
3. Periksa waktu device sudah sesuai

### App Crash Saat Dibuka
- Sudah diperbaiki dengan error handling
- Jika masih crash, lihat logcat stack trace

### Alarm Tidak Trigger
- Periksa Settings → Battery → Background Restriction
- Device Doze mode bisa menunda alarm (gunakan setExactAndAllowWhileIdle)

## Contoh Use Cases

### Pengingat Daily Standup
```kotlin
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 9,
    minute = 30,
    title = "Daily Standup",
    message = "Time for daily standup meeting"
)
```

### Pengingat Minum Air
```kotlin
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 12,
    minute = 0,
    title = "Hydration Reminder",
    message = "Remember to drink water"
)
```

### Pengingat Break
```kotlin
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 15,
    minute = 0,
    title = "Break Time",
    message = "Time for a short break"
)
```

---

**Status**: ✅ Fixed & Tested
**Build**: ✅ SUCCESS
**Crash Issue**: ✅ RESOLVED

