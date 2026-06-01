package de.openkleinanzeigen.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import de.openkleinanzeigen.BuildConfig
import de.openkleinanzeigen.OpenKleinanzeigenApp
import de.openkleinanzeigen.update.UpdateInstaller
import kotlinx.coroutines.flow.first

class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as OpenKleinanzeigenApp
        val settings = app.repos.settings.observeSettings().first()
        if (!settings.autoUpdateEnabled) return Result.success()

        val update = app.repos.updates.checkForUpdate(BuildConfig.VERSION_CODE) ?: return Result.success()
        UpdateInstaller.downloadAndNotify(applicationContext, update)
        return Result.success()
    }
}
