package com.lango.app.domain.model

data class Deck(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isPublic: Boolean = false,
    val userId: String = "",
    val firestoreId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val dueCount: Int = 0,
    val learnedCount: Int = 0,
    val isBuiltIn: Boolean = false,
    val emoji: String = "📚",
    val colorIndex: Int = 0,
    val cloudId: String = ""
)

data class Word(
    val id: Long = 0,
    val deckId: Long,
    val english: String,
    val russian: String,
    val transcription: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    val imageUri: String = "",
    val level: WordLevel = WordLevel.NEW,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val interval: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val cloudId: String = ""
)

enum class WordLevel { NEW, LEARNING, KNOWN }
enum class Rating { AGAIN, HARD, GOOD, EASY }
