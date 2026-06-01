package de.openkleinanzeigen.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.openkleinanzeigen.OpenKleinanzeigenApp
import de.openkleinanzeigen.R
import kotlinx.coroutines.flow.first

class MessageSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OpenKleinanzeigenApp
        val settings = app.repos.settings.observeSettings().first()
        if (!settings.notifyMessages) return Result.success()
        val session = app.repos.auth.currentSession() ?: return Result.success()

        val before = app.repos.messages.observeConversations().first()
        app.repos.messages.refreshConversations()
        val after = app.repos.messages.observeConversations().first()
        val newUnread = after.sumOf { it.unreadCount } - before.sumOf { it.unreadCount }
        if (newUnread > 0) {
            val notification = NotificationCompat.Builder(applicationContext, "messages")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(applicationContext.getString(R.string.messages_title))
                .setContentText("$newUnread new")
                .build()
            applicationContext.getSystemService(NotificationManager::class.java)
                .notify(2001, notification)
        }

        if (settings.notifyBackendAgents) {
            app.repos.backendAgents.syncAgents()
        }
        return Result.success()
    }
}
