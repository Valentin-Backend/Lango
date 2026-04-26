package com.lango.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lango.app.data.local.dao.DeckDao
import com.lango.app.data.local.dao.WordDao
import com.lango.app.data.local.entity.DeckEntity
import com.lango.app.data.local.entity.WordEntity

@Database(entities = [DeckEntity::class, WordEntity::class], version = 5, exportSchema = false)
abstract class LangoDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun wordDao(): WordDao

    companion object {
        @Volatile private var INSTANCE: LangoDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN emoji TEXT NOT NULL DEFAULT '📚'")
                db.execSQL("ALTER TABLE decks ADD COLUMN colorIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE words ADD COLUMN exampleTranslation TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM words WHERE deckId IN (SELECT id FROM decks WHERE isBuiltIn = 1)")
                db.execSQL("DELETE FROM decks WHERE isBuiltIn = 1")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN cloudId TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): LangoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, LangoDatabase::class.java, "lango_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
