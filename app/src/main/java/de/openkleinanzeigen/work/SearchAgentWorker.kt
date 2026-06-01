package de.openkleinanzeigen.work

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.openkleinanzeigen.OpenKleinanzeigenApp
import de.openkleinanzeigen.R
import kotlinx.coroutines.flow.first

class SearchAgentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OpenKleinanzeigenApp
        val settings = app.repos.settings.observeSettings().first()
        if (!settings.notifyAgentMatches) return Result.success()

        val agents = app.repos.searchAgent.observeAgents().first().filter { it.enabled }
        for (agent in agents) {
            val newMatches = app.repos.searchAgent.findNewMatches(agent)
            if (newMatches.isEmpty()) continue
            app.repos.searchAgent.markSeen(agent.id, newMatches.map { it.id }.toSet())

            if (settings.notifyAgentMatches) {
                notifyMatch(agent.name, newMatches.first().title)
            }

            val template = agent.autoMessageTemplate
            val session = app.repos.auth.currentSession()
            if (!template.isNullOrBlank() && session != null) {
                for (listing in newMatches) {
                    try {
                        app.repos.messages.sendMessageToAd(listing.id, template)
                    } catch (_: Exception) {
                        // Gateway may block; continue
                    }
                }
            }
        }
        return Result.success()
    }

    private fun notifyMatch(agentName: String, listingTitle: String) {
        val notification = NotificationCompat.Builder(applicationContext, "agents")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(agentName)
            .setContentText(listingTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(agentName.hashCode(), notification)
    }
}
