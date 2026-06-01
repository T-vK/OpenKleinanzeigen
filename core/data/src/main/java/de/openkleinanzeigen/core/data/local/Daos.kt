package de.openkleinanzeigen.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchAgentDao {
    @Query("SELECT * FROM search_agents ORDER BY name")
    fun observeAll(): Flow<List<SearchAgentEntity>>

    @Query("SELECT * FROM search_agents WHERE id = :id")
    suspend fun getById(id: Long): SearchAgentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(agent: SearchAgentEntity): Long

    @Insert
    suspend fun insert(agent: SearchAgentEntity): Long

    @Query("DELETE FROM search_agents WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SeenListingDao {
    @Query("SELECT listingId FROM seen_listings WHERE agentId = :agentId")
    suspend fun getSeen(agentId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<SeenListingEntity>)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY sentAt ASC")
    fun observe(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)
}
