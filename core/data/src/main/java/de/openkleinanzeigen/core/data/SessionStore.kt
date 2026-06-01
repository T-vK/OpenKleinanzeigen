package de.openkleinanzeigen.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.openkleinanzeigen.core.domain.model.UserSession

class SessionStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(session: UserSession) {
        prefs.edit()
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .apply()
    }

    fun load(): UserSession? {
        val token = prefs.getString("access_token", null) ?: return null
        val userId = prefs.getString("user_id", null) ?: return null
        val email = prefs.getString("email", null) ?: return null
        return UserSession(
            userId = userId,
            email = email,
            accessToken = token,
            refreshToken = prefs.getString("refresh_token", null),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
