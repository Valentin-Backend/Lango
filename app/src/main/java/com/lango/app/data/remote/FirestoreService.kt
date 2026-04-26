package com.lango.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.lango.app.domain.model.Deck
import com.lango.app.domain.model.Word
import com.lango.app.domain.model.WordLevel
import kotlinx.coroutines.tasks.await

object FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun publishDeck(deck: Deck, words: List<Word>, userId: String, userEmail: String): String? {
        return try {
            val deckData = hashMapOf(
                "title" to deck.title,
                "description" to deck.description,
                "userId" to userId,
                "userEmail" to userEmail,
                "wordCount" to words.size,
                "createdAt" to System.currentTimeMillis(),
                "emoji" to deck.emoji,
                "colorIndex" to deck.colorIndex
            )
            val docRef = db.collection("public_decks").add(deckData).await()
            val wordsCol = docRef.collection("words")
            words.forEach { word ->
                val wordData = hashMapOf(
                    "english" to word.english,
                    "russian" to word.russian,
                    "transcription" to word.transcription,
                    "example" to word.example,
                    "exampleTranslation" to word.exampleTranslation,
                    "imageUri" to word.imageUri
                )
                wordsCol.add(wordData).await()
            }
            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPublicDecks(): List<PublicDeckInfo> {
        return try {
            val snapshot = db.collection("public_decks")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    PublicDeckInfo(
                        firestoreId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        wordCount = (doc.getLong("wordCount") ?: 0).toInt(),
                        emoji = doc.getString("emoji") ?: "📚",
                        colorIndex = (doc.getLong("colorIndex") ?: 0).toInt()
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun saveUserDeckAndGetWordIds(userId: String, deck: Deck, words: List<Word>): Pair<String, Map<Long, String>>? {
        return try {
            val deckData = hashMapOf(
                "title" to deck.title,
                "description" to deck.description,
                "emoji" to deck.emoji,
                "colorIndex" to deck.colorIndex,
                "isPublic" to deck.isPublic,
                "firestoreId" to deck.firestoreId,
                "createdAt" to deck.createdAt,
                "localId" to deck.id,
                "wordCount" to words.size
            )
            var alreadyWritten = false
            val docRef = when {
                deck.cloudId.isNotBlank() ->
                    db.collection("users").document(userId).collection("decks").document(deck.cloudId)
                else -> {
                    val existing = db.collection("users").document(userId).collection("decks")
                        .whereEqualTo("localId", deck.id).limit(1).get().await().documents.firstOrNull()
                    if (existing != null) {
                        existing.reference
                    } else {
                        alreadyWritten = true
                        db.collection("users").document(userId).collection("decks").add(deckData).await()
                    }
                }
            }
            if (!alreadyWritten) docRef.set(deckData).await()

            val wordsCol = docRef.collection("words")
            val cloudWordIds = wordsCol.get().await().documents.map { it.id }.toSet()

            val localWithCloudId = words.filter { it.cloudId.isNotBlank() }
            val localWithoutCloudId = words.filter { it.cloudId.isBlank() }

            localWithCloudId.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.set(wordsCol.document(it.cloudId), wordToMap(it)) }
                batch.commit().await()
            }

            val localCloudIds = localWithCloudId.map { it.cloudId }.toSet()
            (cloudWordIds - localCloudIds).chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(wordsCol.document(it)) }
                batch.commit().await()
            }

            val newWordIds = mutableMapOf<Long, String>()
            localWithoutCloudId.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { word ->
                    val ref = wordsCol.document()
                    newWordIds[word.id] = ref.id
                    batch.set(ref, wordToMap(word) )
                }
                batch.commit().await()
            }

            Pair(docRef.id, newWordIds)
        } catch (e: Exception) { null }
    }

    private fun wordToMap(word: Word) = hashMapOf(
        "english" to word.english,
        "russian" to word.russian,
        "transcription" to word.transcription,
        "example" to word.example,
        "exampleTranslation" to word.exampleTranslation,
        "imageUri" to word.imageUri,
        "level" to word.level.name,
        "nextReviewDate" to word.nextReviewDate,
        "interval" to word.interval,
        "easeFactor" to word.easeFactor,
        "repetitions" to word.repetitions
    )

    suspend fun deleteUserDeck(userId: String, cloudId: String) {
        try {
            val ref = db.collection("users").document(userId).collection("decks").document(cloudId)
            val wordDocs = ref.collection("words").get().await().documents
            wordDocs.chunked(500).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            ref.delete().await()
        } catch (_: Exception) {}
    }

    suspend fun getUserDecks(userId: String): List<CloudDeck> {
        return try {
            val snapshot = db.collection("users").document(userId).collection("decks").get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val words = doc.reference.collection("words").get().await().mapNotNull { w ->
                        try {
                            Word(
                                deckId = 0,
                                english = w.getString("english") ?: "",
                                russian = w.getString("russian") ?: "",
                                transcription = w.getString("transcription") ?: "",
                                example = w.getString("example") ?: "",
                                exampleTranslation = w.getString("exampleTranslation") ?: "",
                                imageUri = w.getString("imageUri") ?: "",
                                level = try { WordLevel.valueOf(w.getString("level") ?: "NEW") } catch (_: Exception) { WordLevel.NEW },
                                nextReviewDate = w.getLong("nextReviewDate") ?: System.currentTimeMillis(),
                                interval = (w.getLong("interval") ?: 1).toInt(),
                                easeFactor = (w.getDouble("easeFactor") ?: 2.5).toFloat(),
                                repetitions = (w.getLong("repetitions") ?: 0).toInt(),
                                cloudId = w.id
                            )
                        } catch (_: Exception) { null }
                    }
                    CloudDeck(
                        cloudId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        emoji = doc.getString("emoji") ?: "📚",
                        colorIndex = (doc.getLong("colorIndex") ?: 0).toInt(),
                        isPublic = doc.getBoolean("isPublic") ?: false,
                        firestoreId = doc.getString("firestoreId") ?: "",
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        words = words
                    )
                } catch (_: Exception) { null }
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getPublicDeckWords(firestoreId: String): List<Word> {
        return try {
            val snapshot = db.collection("public_decks")
                .document(firestoreId)
                .collection("words")
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    Word(
                        deckId = 0,
                        english = doc.getString("english") ?: "",
                        russian = doc.getString("russian") ?: "",
                        transcription = doc.getString("transcription") ?: "",
                        example = doc.getString("example") ?: "",
                        exampleTranslation = doc.getString("exampleTranslation") ?: "",
                        imageUri = doc.getString("imageUri") ?: "",
                        level = WordLevel.NEW
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) { emptyList() }
    }
}

data class CloudDeck(
    val cloudId: String,
    val title: String,
    val description: String,
    val emoji: String,
    val colorIndex: Int,
    val isPublic: Boolean,
    val firestoreId: String,
    val createdAt: Long,
    val words: List<Word>
)

data class PublicDeckInfo(
    val firestoreId: String,
    val title: String,
    val description: String,
    val userEmail: String,
    val wordCount: Int,
    val emoji: String = "📚",
    val colorIndex: Int = 0
)
