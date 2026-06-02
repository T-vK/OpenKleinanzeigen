package de.openkleinanzeigen.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SearchAgentEntity::class,
        SeenListingEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchAgentDao(): SearchAgentDao
    abstract fun seenListingDao(): SeenListingDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE search_agents ADD COLUMN locationName TEXT")
                db.execSQL("ALTER TABLE search_agents ADD COLUMN pictureRequired INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE search_agents ADD COLUMN buyNowOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE search_agents ADD COLUMN adType TEXT")
                db.execSQL("ALTER TABLE search_agents ADD COLUMN posterType TEXT")
            }
        }
    }
}
