package de.openkleinanzeigen.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_agents")
data class SearchAgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String,
    val locationId: Int?,
    val locationName: String?,
    val radiusKm: Int?,
    val minPrice: Int?,
    val maxPrice: Int?,
    val categoryId: Int?,
    val pictureRequired: Boolean = false,
    val buyNowOnly: Boolean = false,
    val adType: String?,
    val posterType: String?,
    val enabled: Boolean,
    val autoMessageTemplate: String?,
    val notifyOnMatch: Boolean,
)

@Entity(tableName = "seen_listings", primaryKeys = ["agentId", "listingId"])
data class SeenListingEntity(
    val agentId: Long,
    val listingId: String,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val preview: String?,
    val updatedAt: Long,
    val unreadCount: Int,
    val adId: String?,
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val sentAt: Long,
    val outgoing: Boolean,
)
