package com.lango.app.data.local.dao

import androidx.room.*
import com.lango.app.data.local.entity.DeckEntity
import com.lango.app.data.local.entity.WordEntity
import kotlinx.coroutines.flow.Flow

data class DeckWithStats(
    @Embedded val deck: DeckEntity,
    val wordCount: Int,
    val learnedCount: Int,
    val dueCount: Int
)

@Dao
interface DeckDao {
    @Query(
        """
        SELECT d.*,
          (SELECT COUNT(*) FROM words w WHERE w.deckId = d.id) AS wordCount,
          (SELECT COUNT(*) FROM words w WHERE w.deckId = d.id AND w.level = 'KNOWN') AS learnedCount,
          (SELECT COUNT(*) FROM words w WHERE w.deckId = d.id AND w.nextReviewDate <= :now) AS dueCount
        FROM decks d
        ORDER BY d.createdAt DESC
        """
    )
    fun getAllDecksWithStats(now: Long): Flow<List<DeckWithStats>>

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: Long): DeckEntity?

    @Query("SELECT * FROM decks WHERE firestoreId = :firestoreId LIMIT 1")
    suspend fun getDeckByFirestoreId(firestoreId: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getDeckByCloudId(cloudId: String): DeckEntity?

    @Query("SELECT * FROM decks WHERE isBuiltIn = 0 ORDER BY createdAt DESC")
    suspend fun getAllDecksOnce(): List<DeckEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId")
    suspend fun getWordCount(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId AND level = 'KNOWN'")
    suspend fun getLearnedCount(deckId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE deckId = :deckId AND nextReviewDate <= :now")
    suspend fun getDueCount(deckId: Long, now: Long): Int

    @Query("SELECT COUNT(*) FROM decks WHERE isBuiltIn = 1")
    suspend fun getBuiltInDeckCount(): Int

    @Query("SELECT * FROM decks WHERE isBuiltIn = 1 ORDER BY id ASC")
    suspend fun getBuiltInDecks(): List<DeckEntity>
}

@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY id ASC")
    fun getWordsByDeck(deckId: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getWordsOnce(deckId: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getWordById(id: Long): WordEntity?

    @Query("SELECT * FROM words WHERE deckId = :deckId AND nextReviewDate <= :now ORDER BY nextReviewDate ASC")
    suspend fun getDueWords(deckId: Long, now: Long): List<WordEntity>

    @Query("SELECT * FROM words WHERE deckId = :deckId ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(deckId: Long, limit: Int): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Update
    suspend fun updateWord(word: WordEntity)

    @Delete
    suspend fun deleteWord(word: WordEntity)

    @Query("DELETE FROM words WHERE deckId = :deckId")
    suspend fun deleteWordsByDeck(deckId: Long)
}
