# Notifikasi Terjadwal - Reference

File di folder ini adalah untuk notifikasi terjadwal menggunakan AlarmManager.

## Cara Menggunakan (jika diperlukan)

### 1. Schedule Notifikasi Untuk Jam Tertentu

```kotlin
import com.example.kotlintemplate.notification.NotificationScheduler

// Jadwalkan notifikasi untuk jam 15:00
NotificationScheduler.scheduleNotification(
    context = this,
    hour = 15,
    minute = 0,
    title = "Reminder",
    message = "Meeting dimulai"
)
```

### 2. Tampilkan Notifikasi Langsung (Immediate)

```kotlin
import com.example.kotlintemplate.notification.NotificationHelper

// Tampilkan notifikasi langsung (tanpa menunggu alarm)
NotificationHelper.showNotificationImmediately(
    context = this,
    title = "Test",
    message = "Ini notifikasi langsung"
)
```

## Files

- **NotificationScheduler.kt** - Menjadwalkan alarm untuk waktu tertentu
- **NotificationReceiver.kt** - BroadcastReceiver yang menampilkan notifikasi saat alarm trigger
- **NotificationHelper.kt** - Helper untuk tampilkan notifikasi langsung

## Permission Diperlukan

Di AndroidManifest.xml sudah ditambahkan:
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Untuk Android 13+, permission POST_NOTIFICATIONS harus diminta saat runtime.

## Notes

- Notifikasi tidak bertahan setelah reboot device (perlu BOOT_COMPLETED receiver jika ingin)
- Saat ini tidak recurring otomatis (bisa ditambah manual di receiver)
- Device Doze mode bisa menunda/mencegah alarm trigger

