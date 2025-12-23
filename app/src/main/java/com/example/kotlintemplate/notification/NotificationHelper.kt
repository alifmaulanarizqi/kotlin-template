package com.example.kotlintemplate.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kotlintemplate.R

/**
 * Helper untuk menampilkan notifikasi langsung (immediate)
 * Tidak perlu menunggu alarm trigger
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    const val CHANNEL_ID = "scheduled_notification_channel"
    const val NOTIFICATION_ID = 1001

    /**
     * Tampilkan notifikasi langsung (immediate)
     * Gunakan ini untuk testing notifikasi
     */
    fun showNotificationImmediately(
        context: Context,
        title: String = "Test Notifikasi",
        message: String = "Ini adalah test notifikasi langsung"
    ) {
        try {
            Log.d(TAG, "Attempting to show notification: $title - $message")

            val appContext = context.applicationContext

            // Check permission untuk Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            // Create channel
            createNotificationChannel(appContext)

            // Build notification
            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .build()

            // Show notification
            val notificationManager = NotificationManagerCompat.from(appContext)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "✅ Notifikasi berhasil ditampilkan: $title")

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException - Permission issue", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager != null) {
                    val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                    if (existingChannel == null) {
                        val channel = NotificationChannel(
                            CHANNEL_ID,
                            "Scheduled Notifications",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply {
                            description = "Notifikasi yang dijadwalkan atau langsung"
                            enableVibration(true)
                            setShowBadge(true)
                        }
                        notificationManager.createNotificationChannel(channel)
                        Log.d(TAG, "Notification channel created")
                    } else {
                        Log.d(TAG, "Notification channel already exists")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }
}

