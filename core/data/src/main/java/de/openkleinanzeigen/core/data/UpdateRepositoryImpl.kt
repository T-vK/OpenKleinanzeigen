package de.openkleinanzeigen.core.data

import de.openkleinanzeigen.core.domain.model.UpdateInfo
import de.openkleinanzeigen.core.domain.repository.UpdateRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class UpdateRepositoryImpl(
    private val http: OkHttpClient = OkHttpClient(),
    private val repoUrl: String = "https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo/index-v1.json",
) : UpdateRepository {
    override suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
        val request = Request.Builder().url(repoUrl).get().build()
        val fdroidResult = http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                null
            } else {
                response.body?.string()?.let { parseFdroidIndex(it, currentVersionCode) }
            }
        }
        return fdroidResult ?: checkGitHubReleases(currentVersionCode)
    }

    private fun checkGitHubReleases(currentVersionCode: Int): UpdateInfo? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/T-vK/OpenKleinanzeigen/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        return http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@use null
            }
            val body = response.body?.string() ?: return@use null
            val json = Json.parseToJsonElement(body).jsonObject
            val tag = json["tag_name"]?.jsonPrimitive?.contentOrNull ?: return@use null
            val assets = json["assets"]?.toString() ?: return@use null
            val apkUrl = Regex("""browser_download_url"\s*:\s*"([^"]+\.apk)""")
                .find(assets)?.groupValues?.get(1) ?: return@use null
            val versionName = tag.removePrefix("v")
            val versionCode = versionName.split('.').let {
                (it.getOrNull(0)?.toIntOrNull() ?: 0) * 10000 +
                    (it.getOrNull(1)?.toIntOrNull() ?: 0) * 100 +
                    (it.getOrNull(2)?.toIntOrNull() ?: 0)
            }
            if (versionCode <= currentVersionCode) {
                return@use null
            }
            UpdateInfo(versionCode, versionName, apkUrl)
        }
    }

    private fun parseFdroidIndex(body: String, currentVersionCode: Int): UpdateInfo? {
        val json = Json.parseToJsonElement(body).jsonObject
        val packages = json["packages"]?.jsonObject ?: return null
        val app = packages["de.openkleinanzeigen"]?.jsonObject ?: return null
        val versions = app["versions"]?.jsonObject ?: return null
        val latestKey = versions.keys.maxByOrNull { key ->
            versions[key]?.jsonObject?.get("versionCode")?.jsonPrimitive?.intOrNull ?: 0
        } ?: return null
        val version = versions[latestKey]?.jsonObject ?: return null
        val versionCode = version["versionCode"]?.jsonPrimitive?.intOrNull ?: return null
        if (versionCode <= currentVersionCode) return null
        val versionName = version["versionName"]?.jsonPrimitive?.contentOrNull ?: latestKey
        val apkUrl = "https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo/$latestKey"
        return UpdateInfo(versionCode, versionName, apkUrl)
    }
}
