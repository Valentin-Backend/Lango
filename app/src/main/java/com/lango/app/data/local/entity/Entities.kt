package com.lango.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lango.app.domain.model.*

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val isPublic: Boolean = false,
    val userId: String = "",
    val firestoreId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false,
    val emoji: String = "📚",
    val colorIndex: Int = 0,
    val cloudId: String = ""
) {
    fun toDomain(wordCount: Int = 0, dueCount: Int = 0, learnedCount: Int = 0) = Deck(
        id = id, title = title, description = description, isPublic = isPublic,
        userId = userId, firestoreId = firestoreId, createdAt = createdAt,
        wordCount = wordCount, dueCount = dueCount, learnedCount = learnedCount,
        isBuiltIn = isBuiltIn, emoji = emoji, colorIndex = colorIndex, cloudId = cloudId
    )
}

fun Deck.toEntity() = DeckEntity(
    id = id, title = title, description = description, isPublic = isPublic,
    userId = userId, firestoreId = firestoreId, createdAt = createdAt,
    isBuiltIn = isBuiltIn, emoji = emoji, colorIndex = colorIndex, cloudId = cloudId
)

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val english: String,
    val russian: String,
    val transcription: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    val imageUri: String = "",
    val level: String = "NEW",
    val nextReviewDate: Long = System.currentTimeMillis(),
    val interval: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val cloudId: String = ""
) {
    fun toDomain() = Word(
        id = id, deckId = deckId, english = english, russian = russian,
        transcription = transcription, example = example, exampleTranslation = exampleTranslation,
        imageUri = imageUri, level = WordLevel.valueOf(level), nextReviewDate = nextReviewDate,
        interval = interval, easeFactor = easeFactor, repetitions = repetitions,
        cloudId = cloudId
    )
}

fun Word.toEntity() = WordEntity(
    id = id, deckId = deckId, english = english, russian = russian,
    transcription = transcription, example = example, exampleTranslation = exampleTranslation,
    imageUri = imageUri, level = level.name, nextReviewDate = nextReviewDate,
    interval = interval, easeFactor = easeFactor, repetitions = repetitions,
    cloudId = cloudId
)
