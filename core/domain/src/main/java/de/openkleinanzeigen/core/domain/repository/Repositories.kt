package de.openkleinanzeigen.core.domain.repository

import de.openkleinanzeigen.core.domain.model.AppSettings
import de.openkleinanzeigen.core.domain.model.BackendSearchAgent
import de.openkleinanzeigen.core.domain.model.Category
import de.openkleinanzeigen.core.domain.model.ChatMessage
import de.openkleinanzeigen.core.domain.model.Conversation
import de.openkleinanzeigen.core.domain.model.Listing
import de.openkleinanzeigen.core.domain.model.LocalSearchAgent
import de.openkleinanzeigen.core.domain.model.Location
import de.openkleinanzeigen.core.domain.model.SearchQuery
import de.openkleinanzeigen.core.domain.model.UpdateInfo
import de.openkleinanzeigen.core.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface ListingRepository {
    suspend fun search(query: SearchQuery): List<Listing>
    suspend fun getListing(id: String): Listing
    suspend fun searchLocations(query: String, limit: Int = 15): List<Location>
    suspend fun getTopLocations(): List<Location>
    suspend fun getCategories(): List<Category>
}

interface SearchAgentRepository {
    fun observeAgents(): Flow<List<LocalSearchAgent>>
    suspend fun getAgent(id: Long): LocalSearchAgent?
    suspend fun saveAgent(agent: LocalSearchAgent): Long
    suspend fun deleteAgent(id: Long)
    suspend fun getSeenListingIds(agentId: Long): Set<String>
    suspend fun markSeen(agentId: Long, listingIds: Set<String>)
    suspend fun findNewMatches(agent: LocalSearchAgent): List<Listing>
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}

interface AuthRepository {
    fun observeSession(): Flow<UserSession?>
    suspend fun login(email: String, password: String): UserSession
    suspend fun logout()
    suspend fun currentSession(): UserSession?
}

interface MessageRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<ChatMessage>>
    suspend fun refreshConversations()
    suspend fun refreshMessages(conversationId: String)
    suspend fun sendMessage(conversationId: String, text: String)
    suspend fun sendMessageToAd(adId: String, text: String)
}

interface BackendAgentRepository {
    suspend fun syncAgents(): List<BackendSearchAgent>
}

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo?
}
