package com.ilvir.telegramwatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Держит процесс живым, чтобы TDLib мог получать апдейты (новые сообщения)
 * даже когда экраны приложения закрыты. На маленьких часах агрессивный
 * energy saver может всё равно убивать процесс — см. инструкцию в чате
 * про "Без ограничений" для батареи и whitelist deviceidle.
 */
class TdlibBackgroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "tg_watch_bg"
        private const val NOTIF_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, TdlibBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // На случай, если сервис стартует первым (например, после перезагрузки)
        TdlibClient.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_running))
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
