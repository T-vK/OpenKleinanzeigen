package de.openkleinanzeigen.core.data

import android.content.Context
import androidx.room.Room
import de.openkleinanzeigen.core.api.KleinanzeigenApiClient
import de.openkleinanzeigen.core.data.local.AppDatabase
import de.openkleinanzeigen.core.data.local.ConversationEntity
import de.openkleinanzeigen.core.data.local.MessageEntity
import de.openkleinanzeigen.core.data.local.SearchAgentEntity
import de.openkleinanzeigen.core.data.local.SeenListingEntity
import de.openkleinanzeigen.core.domain.model.AppSettings
import de.openkleinanzeigen.core.domain.model.BackendSearchAgent
import de.openkleinanzeigen.core.domain.model.ChatMessage
import de.openkleinanzeigen.core.domain.model.Conversation
import de.openkleinanzeigen.core.domain.model.Listing
import de.openkleinanzeigen.core.domain.model.LocalSearchAgent
import de.openkleinanzeigen.core.domain.model.SearchQuery
import de.openkleinanzeigen.core.domain.model.UpdateInfo
import de.openkleinanzeigen.core.domain.model.UserSession
import de.openkleinanzeigen.core.domain.repository.AuthRepository
import de.openkleinanzeigen.core.domain.repository.BackendAgentRepository
import de.openkleinanzeigen.core.domain.repository.ListingRepository
import de.openkleinanzeigen.core.domain.repository.MessageRepository
import de.openkleinanzeigen.core.domain.repository.SearchAgentRepository
import de.openkleinanzeigen.core.domain.repository.SettingsRepository
import de.openkleinanzeigen.core.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request

class AppRepositories(context: Context) {
    private val api = KleinanzeigenApiClient()
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "openkleinanzeigen.db")
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
    private val settingsStore = SettingsDataStore(context)
    private val sessionStore = SessionStore(context)
    private val sessionFlow = MutableStateFlow(sessionStore.load())

    val listing: ListingRepository = ListingRepositoryImpl(api)
    val searchAgent: SearchAgentRepository = SearchAgentRepositoryImpl(api, db)
    val settings: SettingsRepository = SettingsRepositoryImpl(settingsStore)
    val auth: AuthRepository = AuthRepositoryImpl(api, sessionStore, sessionFlow)
    val messages: MessageRepository = MessageRepositoryImpl(api, db, sessionFlow)
    val backendAgents: BackendAgentRepository = BackendAgentRepositoryImpl(api, sessionFlow)
    val updates: UpdateRepository = UpdateRepositoryImpl()
}

private class ListingRepositoryImpl(private val api: KleinanzeigenApiClient) : ListingRepository {
    override suspend fun search(query: SearchQuery) = api.search(query)
    override suspend fun getListing(id: String) = api.getListing(id)
    override suspend fun searchLocations(query: String, limit: Int) = api.searchLocations(query, limit)
    override suspend fun getTopLocations() = api.getTopLocations()
    override suspend fun getCategories() = api.getCategories()
}

private class SearchAgentRepositoryImpl(
    private val api: KleinanzeigenApiClient,
    private val db: AppDatabase,
) : SearchAgentRepository {
    override fun observeAgents(): Flow<List<LocalSearchAgent>> =
        db.searchAgentDao().observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAgent(id: Long) = db.searchAgentDao().getById(id)?.toDomain()

    override suspend fun saveAgent(agent: LocalSearchAgent): Long {
        val entity = agent.toEntity()
        return if (agent.id == 0L) {
            db.searchAgentDao().insert(entity.copy(id = 0))
        } else {
            db.searchAgentDao().upsert(entity)
            agent.id
        }
    }

    override suspend fun deleteAgent(id: Long) = db.searchAgentDao().delete(id)

    override suspend fun getSeenListingIds(agentId: Long): Set<String> =
        db.seenListingDao().getSeen(agentId).toSet()

    override suspend fun markSeen(agentId: Long, listingIds: Set<String>) {
        db.seenListingDao().insertAll(
            listingIds.map { SeenListingEntity(agentId, it) },
        )
    }

    override suspend fun findNewMatches(agent: LocalSearchAgent): List<Listing> {
        val results = api.search(agent.query)
        val seen = getSeenListingIds(agent.id)
        return results.filter { it.id !in seen }
    }
}

private class SettingsRepositoryImpl(private val store: SettingsDataStore) : SettingsRepository {
    override fun observeSettings(): Flow<AppSettings> = store.settings
    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        var debug = false
        store.update { current ->
            val updated = transform(current)
            debug = updated.debugLoggingEnabled
            updated
        }
        de.openkleinanzeigen.core.common.AppLogger.setEnabled(debug)
    }
}

private class AuthRepositoryImpl(
    private val api: KleinanzeigenApiClient,
    private val sessionStore: SessionStore,
    private val sessionFlow: MutableStateFlow<UserSession?>,
) : AuthRepository {
    override fun observeSession(): Flow<UserSession?> = sessionFlow
    override suspend fun login(email: String, password: String): UserSession {
        val session = api.login(email, password)
        sessionStore.save(session)
        sessionFlow.value = session
        return session
    }
    override suspend fun logout() {
        sessionStore.clear()
        sessionFlow.value = null
    }
    override suspend fun currentSession(): UserSession? = sessionFlow.value
}

private class MessageRepositoryImpl(
    private val api: KleinanzeigenApiClient,
    private val db: AppDatabase,
    private val sessionFlow: MutableStateFlow<UserSession?>,
) : MessageRepository {
    override fun observeConversations(): Flow<List<Conversation>> =
        db.conversationDao().observeAll().map { list ->
            list.map { Conversation(it.id, it.title, it.preview, it.updatedAt, it.unreadCount, it.adId) }
        }

    override fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        db.messageDao().observe(conversationId).map { list ->
            list.map { ChatMessage(it.id, it.conversationId, it.text, it.sentAt, it.outgoing) }
        }

    override suspend fun refreshConversations() {
        val session = sessionFlow.value ?: return
        val convos = api.getConversations(session)
        db.conversationDao().upsertAll(
            convos.map {
                ConversationEntity(it.id, it.title, it.preview, it.updatedAt, it.unreadCount, it.adId)
            },
        )
    }

    override suspend fun refreshMessages(conversationId: String) {
        val session = sessionFlow.value ?: return
        val messages = api.getMessages(session, conversationId)
        db.messageDao().upsertAll(
            messages.map {
                MessageEntity(it.id, it.conversationId, it.text, it.sentAt, it.outgoing)
            },
        )
    }

    override suspend fun sendMessage(conversationId: String, text: String) {
        val session = sessionFlow.value ?: return
        api.sendMessage(session, conversationId, text)
        refreshMessages(conversationId)
    }

    override suspend fun sendMessageToAd(adId: String, text: String) {
        val session = sessionFlow.value ?: return
        api.sendMessageToAd(session, adId, text)
    }
}

private class BackendAgentRepositoryImpl(
    private val api: KleinanzeigenApiClient,
    private val sessionFlow: MutableStateFlow<UserSession?>,
) : BackendAgentRepository {
    override suspend fun syncAgents(): List<BackendSearchAgent> {
        val session = sessionFlow.value ?: return emptyList()
        return api.getBackendSearchAgents(session)
    }
}

private fun SearchAgentEntity.toDomain() = LocalSearchAgent(
    id = id,
    name = name,
    query = SearchQuery(
        query = query,
        locationId = locationId,
        locationName = locationName,
        radiusKm = radiusKm,
        minPrice = minPrice,
        maxPrice = maxPrice,
        categoryId = categoryId,
        pictureRequired = pictureRequired,
        buyNowOnly = buyNowOnly,
        adType = adType?.let { runCatching { de.openkleinanzeigen.core.domain.model.AdType.valueOf(it) }.getOrNull() },
        posterType = posterType?.let { runCatching { de.openkleinanzeigen.core.domain.model.PosterType.valueOf(it) }.getOrNull() },
    ),
    enabled = enabled,
    autoMessageTemplate = autoMessageTemplate,
    notifyOnMatch = notifyOnMatch,
)

private fun LocalSearchAgent.toEntity() = SearchAgentEntity(
    id = id,
    name = name,
    query = query.query,
    locationId = query.locationId,
    locationName = query.locationName,
    radiusKm = query.radiusKm,
    minPrice = query.minPrice,
    maxPrice = query.maxPrice,
    categoryId = query.categoryId,
    pictureRequired = query.pictureRequired,
    buyNowOnly = query.buyNowOnly,
    adType = query.adType?.name,
    posterType = query.posterType?.name,
    enabled = enabled,
    autoMessageTemplate = autoMessageTemplate,
    notifyOnMatch = notifyOnMatch,
)
