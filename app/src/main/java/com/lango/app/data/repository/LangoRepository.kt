package com.lango.app.data.repository

import com.lango.app.data.local.dao.DeckDao
import com.lango.app.data.local.dao.WordDao
import com.lango.app.data.local.entity.DeckEntity
import com.lango.app.data.local.entity.toEntity
import com.lango.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

class LangoRepository(
    private val deckDao: DeckDao,
    private val wordDao: WordDao
) {
    fun getAllDecks(): Flow<List<Deck>> =
        deckDao.getAllDecksWithStats(System.currentTimeMillis())
            .map { rows ->
                rows.map { row ->
                    row.deck.toDomain(row.wordCount, row.dueCount, row.learnedCount)
                }
            }

    suspend fun getDeckById(id: Long): Deck? {
        val entity = deckDao.getDeckById(id) ?: return null
        val wordCount = deckDao.getWordCount(id)
        val dueCount = deckDao.getDueCount(id, System.currentTimeMillis())
        val learnedCount = deckDao.getLearnedCount(id)
        return entity.toDomain(wordCount, dueCount, learnedCount)
    }

    suspend fun getDeckByFirestoreId(firestoreId: String): Deck? {
        val entity = deckDao.getDeckByFirestoreId(firestoreId) ?: return null
        val wordCount = deckDao.getWordCount(entity.id)
        val dueCount = deckDao.getDueCount(entity.id, System.currentTimeMillis())
        val learnedCount = deckDao.getLearnedCount(entity.id)
        return entity.toDomain(wordCount, dueCount, learnedCount)
    }

    suspend fun getDeckByCloudId(cloudId: String): Deck? {
        val entity = deckDao.getDeckByCloudId(cloudId) ?: return null
        val wordCount = deckDao.getWordCount(entity.id)
        val dueCount = deckDao.getDueCount(entity.id, System.currentTimeMillis())
        val learnedCount = deckDao.getLearnedCount(entity.id)
        return entity.toDomain(wordCount, dueCount, learnedCount)
    }

    suspend fun clearAllUserDecks() {
        val allDecks = deckDao.getAllDecksOnce()
        allDecks.forEach { entity ->
            wordDao.deleteWordsByDeck(entity.id)
            deckDao.deleteDeck(entity)
        }
    }

    suspend fun getAllDecksOnce(): List<Deck> =
        deckDao.getAllDecksOnce().map { entity ->
            val wc = deckDao.getWordCount(entity.id)
            val dc = deckDao.getDueCount(entity.id, System.currentTimeMillis())
            val lc = deckDao.getLearnedCount(entity.id)
            entity.toDomain(wc, dc, lc)
        }

    suspend fun getWordsOnce(deckId: Long): List<Word> =
        wordDao.getWordsOnce(deckId).map { it.toDomain() }

    suspend fun deleteWordsByDeck(deckId: Long) = wordDao.deleteWordsByDeck(deckId)

    suspend fun insertDeck(deck: Deck): Long = deckDao.insertDeck(deck.toEntity())

    suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck.toEntity())

    suspend fun deleteDeck(deck: Deck) {
        wordDao.deleteWordsByDeck(deck.id)
        deckDao.deleteDeck(deck.toEntity())
    }

    fun getWordsByDeck(deckId: Long): Flow<List<Word>> =
        wordDao.getWordsByDeck(deckId).map { it.map { e -> e.toDomain() } }

    suspend fun getWordById(id: Long): Word? = wordDao.getWordById(id)?.toDomain()

    suspend fun insertWord(word: Word): Long = wordDao.insertWord(word.toEntity())

    suspend fun insertWords(words: List<Word>) = wordDao.insertWords(words.map { it.toEntity() })

    suspend fun updateWord(word: Word) = wordDao.updateWord(word.toEntity())

    suspend fun deleteWord(word: Word) = wordDao.deleteWord(word.toEntity())

    suspend fun getTrainingWords(deckId: Long): List<Word> {
        val due = wordDao.getDueWords(deckId, System.currentTimeMillis())
        return if (due.isEmpty()) wordDao.getRandomWords(deckId, 20).map { it.toDomain() }
        else due.map { it.toDomain() }
    }

    fun calculateNextReview(word: Word, rating: Rating): Word {
        val now = System.currentTimeMillis()
        return when (rating) {
            Rating.AGAIN -> word.copy(
                interval = 1,
                repetitions = 0,
                easeFactor = max(1.3f, word.easeFactor - 0.2f),
                nextReviewDate = now + TimeUnit.DAYS.toMillis(1),
                level = WordLevel.LEARNING
            )
            Rating.HARD -> {
                val newInterval = max(1, (word.interval * 1.2f).roundToInt())
                word.copy(
                    interval = newInterval,
                    easeFactor = max(1.3f, word.easeFactor - 0.15f),
                    nextReviewDate = now + TimeUnit.DAYS.toMillis(newInterval.toLong()),
                    level = WordLevel.LEARNING
                )
            }
            Rating.GOOD -> {
                val newInterval = if (word.repetitions == 0) 1
                else if (word.repetitions == 1) 4
                else (word.interval * word.easeFactor).roundToInt()
                val newReps = word.repetitions + 1
                word.copy(
                    interval = newInterval,
                    repetitions = newReps,
                    nextReviewDate = now + TimeUnit.DAYS.toMillis(newInterval.toLong()),
                    level = if (newReps >= 5) WordLevel.KNOWN else WordLevel.LEARNING
                )
            }
            Rating.EASY -> {
                val newEase = word.easeFactor + 0.15f
                val newInterval = (word.interval * newEase * 1.3f).roundToInt()
                val newReps = word.repetitions + 1
                word.copy(
                    interval = newInterval,
                    easeFactor = newEase,
                    repetitions = newReps,
                    nextReviewDate = now + TimeUnit.DAYS.toMillis(newInterval.toLong()),
                    level = WordLevel.KNOWN
                )
            }
        }
    }
}
