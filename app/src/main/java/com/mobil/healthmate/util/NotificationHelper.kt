package com.mobil.healthmate.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mobil.healthmate.R
import com.mobil.healthmate.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "health_mate_smart_channel"
        const val CHANNEL_NAME = "Health Mate Asistanı"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // ÖNEMLİ: Bildirimin ekranda belirmesi (Heads-up) için HIGH olmalı
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sağlık hedefleri ve hatırlatmalar"
                enableVibration(true) // Titreşimi aktif et
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(title: String, message: String, id: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // İkonun doğru olduğundan emin ol (yoksa kare çıkar)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Uzun metinler için genişleyebilen stil
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Android 7.1 ve altı için yüksek öncelik
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Ses ve titreşim varsayılanlarını kullan

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }
}