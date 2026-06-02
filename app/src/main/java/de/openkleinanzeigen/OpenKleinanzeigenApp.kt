package de.openkleinanzeigen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import de.openkleinanzeigen.core.common.AppLogger
import de.openkleinanzeigen.core.data.AppRepositories
import de.openkleinanzeigen.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class OpenKleinanzeigenApp : Application() {
    lateinit var repos: AppRepositories
        private set

    override fun onCreate() {
        super.onCreate()
        applyLocale()
        AppLogger.init(File(filesDir, "debug.log"))
        repos = AppRepositories(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repos.settings.observeSettings().collect { settings ->
                AppLogger.setEnabled(settings.debugLoggingEnabled)
            }
        }
        createNotificationChannels()
        WorkScheduler.scheduleAll(this)
    }

    private fun applyLocale() {
        val systemLang = Locale.getDefault().language
        val tag = if (systemLang == "de") "de" else "en"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        listOf(
            "agents" to getString(R.string.notification_channel_agents),
            "messages" to getString(R.string.notification_channel_messages),
            "updates" to getString(R.string.notification_channel_updates),
        ).forEach { (id, name) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
