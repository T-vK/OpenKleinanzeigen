package de.openkleinanzeigen.core.domain.model

data class Listing(
    val id: String,
    val title: String,
    val priceLabel: String?,
    val location: String?,
    val imageUrl: String?,
    val webUrl: String?,
    val description: String? = null,
)

data class SearchQuery(
    val query: String = "",
    val locationId: Int? = null,
    val radiusKm: Int? = null,
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val categoryId: Int? = null,
    val page: Int = 0,
    val size: Int = 30,
    val pictureRequired: Boolean = false,
)

data class Location(
    val id: Int,
    val name: String,
)

data class Category(
    val id: Int,
    val name: String,
)

data class LocalSearchAgent(
    val id: Long = 0,
    val name: String,
    val query: SearchQuery,
    val enabled: Boolean = true,
    val autoMessageTemplate: String? = null,
    val notifyOnMatch: Boolean = true,
)

data class BackendSearchAgent(
    val id: String,
    val name: String,
    val querySummary: String,
    val enabled: Boolean,
)

data class Conversation(
    val id: String,
    val title: String,
    val preview: String?,
    val updatedAt: Long,
    val unreadCount: Int,
    val adId: String?,
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val text: String,
    val sentAt: Long,
    val outgoing: Boolean,
)

data class UserSession(
    val userId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String? = null,
)

data class AppSettings(
    val pollIntervalSeconds: Long = 300,
    val updateCheckIntervalSeconds: Long = 5,
    val autoUpdateEnabled: Boolean = false,
    val debugLoggingEnabled: Boolean = false,
    val notifyNewListings: Boolean = true,
    val notifyMessages: Boolean = true,
    val notifyAgentMatches: Boolean = true,
    val notifyBackendAgents: Boolean = true,
)

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
)
