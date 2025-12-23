package com.example.kotlintemplate.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kotlintemplate.R

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "NotificationReceiver"
        const val CHANNEL_ID = "scheduled_notification_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        try {
            if (intent == null) {
                Log.w(TAG, "Intent is null")
                return
            }

            // Ekstrak data dari intent
            val title = intent.getStringExtra("title") ?: "Pengingat"
            val message = intent.getStringExtra("message") ?: "Anda memiliki notifikasi"

            // Cek permission POST_NOTIFICATIONS (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            // Buat notification channel
            createNotificationChannel(context)

            // Buat notifikasi
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            // Tampilkan notifikasi
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Notifikasi ditampilkan: $title")

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
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Scheduled Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifikasi yang dijadwalkan"
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }
}
