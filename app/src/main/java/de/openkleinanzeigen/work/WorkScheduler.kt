package de.openkleinanzeigen.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import de.openkleinanzeigen.OpenKleinanzeigenApp
import de.openkleinanzeigen.update.UpdateForegroundService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

object WorkScheduler {
    fun scheduleAll(context: Context) {
        val app = context.applicationContext as OpenKleinanzeigenApp
        val settings = runBlocking { app.repos.settings.observeSettings().first() }
        val pollMinutes = (settings.pollIntervalSeconds / 60).coerceAtLeast(1)

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "search_agents",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SearchAgentWorker>(pollMinutes, TimeUnit.MINUTES)
                .build(),
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "messages",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
                .build(),
        )
        if (settings.autoUpdateEnabled) {
            val updateSeconds = settings.updateCheckIntervalSeconds.coerceAtLeast(5)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "app_update",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<UpdateCheckWorker>(updateSeconds, TimeUnit.SECONDS)
                    .build(),
            )
            UpdateForegroundService.start(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("app_update")
            UpdateForegroundService.stop(context)
        }
    }
}
