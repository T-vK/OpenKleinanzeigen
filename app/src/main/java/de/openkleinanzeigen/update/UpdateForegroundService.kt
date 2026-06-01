package de.openkleinanzeigen.update

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.openkleinanzeigen.R

/**
 * Keeps the process eligible for frequent update checks while auto-update is enabled.
 * Actual checks run via [de.openkleinanzeigen.work.UpdateCheckWorker].
 */
class UpdateForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "updates")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.update_downloading))
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(4001, notification)
        return START_STICKY
    }

    companion object {
        fun start(context: Context) {
            context.startForegroundService(Intent(context, UpdateForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UpdateForegroundService::class.java))
        }
    }
}
