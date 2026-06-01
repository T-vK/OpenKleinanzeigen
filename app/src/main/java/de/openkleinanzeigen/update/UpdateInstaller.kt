package de.openkleinanzeigen.update

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import de.openkleinanzeigen.BuildConfig
import de.openkleinanzeigen.R
import de.openkleinanzeigen.core.common.AppLogger
import de.openkleinanzeigen.core.domain.model.UpdateInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object UpdateInstaller {
    private val http = OkHttpClient()

    fun downloadAndNotify(context: Context, update: UpdateInfo) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(dir, "OpenKleinanzeigen-${update.versionName}.apk")
        try {
            val request = Request.Builder().url(update.apkUrl).get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return
                response.body?.byteStream()?.use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            showInstallNotification(context, apkFile, update.versionName)
        } catch (e: Exception) {
            AppLogger.e("UpdateInstaller", "Download failed", e)
        }
    }

    private fun showInstallNotification(context: Context, apkFile: File, versionName: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, "updates")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.update_available))
            .setContentText(versionName)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(3001, notification)
    }
}
