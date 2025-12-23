package com.example.kotlintemplate.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    /**
     * Jadwalkan notifikasi untuk jam tertentu hari ini. Jika waktu sudah lewat, jadwalkan untuk besok.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleNotification(
        context: Context,
        hour: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        try {
            val appContext = context.applicationContext

            // Hitung waktu target
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            // Jika waktu sudah lewat, pindah ke besok
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Buat intent untuk notifikasi
            val intent = Intent(appContext, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("message", message)
            }

            // Buat PendingIntent dengan FLAG_IMMUTABLE (lebih aman)
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getBroadcast(
                    appContext,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    appContext,
                    System.currentTimeMillis().toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            // Set alarm
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ memerlukan permission SCHEDULE_EXACT_ALARM
                    try {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Notifikasi dijadwalkan untuk ${calendar.time}")
                    } catch (e: Exception) {
                        // Fallback ke setAndAllowWhileIdle jika setExact gagal
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Notifikasi dijadwalkan (fallback) untuk ${calendar.time}")
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Notifikasi dijadwalkan untuk ${calendar.time}")
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Notifikasi dijadwalkan untuk ${calendar.time}")
                }
            } else {
                Log.e(TAG, "AlarmManager tidak tersedia")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification", e)
        }
    }
}
