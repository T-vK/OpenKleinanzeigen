package de.openkleinanzeigen.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SearchAgentEntity::class,
        SeenListingEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchAgentDao(): SearchAgentDao
    abstract fun seenListingDao(): SeenListingDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
