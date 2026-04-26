package com.lango.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.lango.app.LangoApplication
import com.lango.app.data.remote.FirestoreService
import com.lango.app.data.remote.GroqApiService
import com.lango.app.data.remote.ImageApiService
import com.lango.app.data.remote.PublicDeckInfo
import com.lango.app.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import android.content.Context

sealed class AuthState {
    object Loading : AuthState()
    data class Success(val userId: String) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val auth = FirebaseAuth.getInstance()
    private val repo = (app as LangoApplication).repository

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    private val prefs = app.getSharedPreferences("lango_auth", Context.MODE_PRIVATE)

    init {
        val user = auth.currentUser
        _state.value = if (user != null) AuthState.Success(user.uid) else AuthState.Unauthenticated
    }

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val newUid = result.user!!.uid
                val lastUid = prefs.getString("last_uid", null)
                if (lastUid != null && lastUid != newUid) {
                    viewModelScope.launch {
                        repo.clearAllUserDecks()
                        prefs.edit().putString("last_uid", newUid).apply()
                        _state.value = AuthState.Success(newUid)
                    }
                } else {
                    prefs.edit().putString("last_uid", newUid).apply()
                    _state.value = AuthState.Success(newUid)
                }
            }
            .addOnFailureListener { ex ->
                _state.value = AuthState.Error(mapFirebaseError(ex, isLogin = true))
            }
    }

    fun register(email: String, password: String, login: String) {
        _state.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null && login.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(login.trim())
                        .build()
                    user.updateProfile(profileUpdates)
                }
                val newUid = user?.uid ?: ""
                val lastUid = prefs.getString("last_uid", null)
                if (lastUid != null && lastUid != newUid) {
                    viewModelScope.launch {
                        repo.clearAllUserDecks()
                        prefs.edit().putString("last_uid", newUid).apply()
                        _state.value = AuthState.Success(newUid)
                    }
                } else {
                    prefs.edit().putString("last_uid", newUid).apply()
                    _state.value = AuthState.Success(newUid)
                }
            }
            .addOnFailureListener { ex ->
                _state.value = AuthState.Error(mapFirebaseError(ex, isLogin = false))
            }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }

    fun continueAsGuest() {
        val lastUid = prefs.getString("last_uid", null)
        if (lastUid != null && lastUid != "guest") {
            viewModelScope.launch {
                repo.clearAllUserDecks()
                prefs.edit().putString("last_uid", "guest").apply()
                _state.value = AuthState.Success("guest")
            }
        } else {
            prefs.edit().putString("last_uid", "guest").apply()
            _state.value = AuthState.Success("guest")
        }
    }

    fun logout() {
        auth.signOut()
        _state.value = AuthState.Unauthenticated
    }

    val currentUser get() = auth.currentUser

    private fun mapFirebaseError(ex: Exception, isLogin: Boolean): String {
        val fb = ex as? FirebaseAuthException
        return when (fb?.errorCode) {
            "ERROR_INVALID_EMAIL" ->
                "Некорректный email. Проверьте адрес и попробуйте снова."
            "ERROR_USER_NOT_FOUND" ->
                "Пользователь с таким email не найден."
            "ERROR_WRONG_PASSWORD" ->
                "Неверный пароль. Попробуйте ещё раз."
            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Аккаунт с таким email уже существует. Войдите или используйте другой адрес."
            "ERROR_WEAK_PASSWORD" ->
                "Слишком простой пароль. Используйте не менее 6 символов."
            "ERROR_TOO_MANY_REQUESTS" ->
                "Слишком много попыток. Подождите немного и попробуйте снова."
            else -> {
                if (isLogin) {
                    "Не удалось войти. Проверьте данные и подключение к интернету."
                } else {
                    "Не удалось создать аккаунт. Попробуйте ещё раз."
                }
            }
        }
    }
}

class DecksViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as LangoApplication).repository
    private val auth = FirebaseAuth.getInstance()
    private val syncMutex = Mutex()

    val decks: StateFlow<List<Deck>> = repo.getAllDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun syncFromCloud() = viewModelScope.launch {
        if (!syncMutex.tryLock()) return@launch
        try {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val cloudDecks = FirestoreService.getUserDecks(userId)

                val localDecks = repo.getAllDecksOnce()
                val localByCloudId = localDecks.filter { it.cloudId.isNotBlank() }.associateBy { it.cloudId }
                val cloudIds = cloudDecks.map { it.cloudId }.toSet()

                if (cloudDecks.isNotEmpty()) {
                    localDecks.forEach { local ->
                        if (local.cloudId.isNotBlank() && local.cloudId !in cloudIds) {
                            repo.deleteDeck(local)
                        }
                    }
                }

                cloudDecks.forEach { cloudDeck ->
                    val existing = localByCloudId[cloudDeck.cloudId]
                    val deckId = if (existing != null) {
                        repo.updateDeck(existing.copy(
                            title = cloudDeck.title,
                            description = cloudDeck.description,
                            emoji = cloudDeck.emoji,
                            colorIndex = cloudDeck.colorIndex,
                            cloudId = cloudDeck.cloudId
                        ))
                        existing.id
                    } else {
                        repo.insertDeck(Deck(
                            title = cloudDeck.title,
                            description = cloudDeck.description,
                            emoji = cloudDeck.emoji,
                            colorIndex = cloudDeck.colorIndex,
                            isPublic = cloudDeck.isPublic,
                            firestoreId = cloudDeck.firestoreId,
                            createdAt = cloudDeck.createdAt,
                            cloudId = cloudDeck.cloudId
                        ))
                    }

                    val localWords = repo.getWordsOnce(deckId)
                    val localByWordCloudId = localWords.filter { it.cloudId.isNotBlank() }.associateBy { it.cloudId }
                    val cloudWordIds = cloudDeck.words.map { it.cloudId }.toSet()

                    localWords.forEach { local ->
                        if (local.cloudId.isNotBlank() && local.cloudId !in cloudWordIds) {
                            repo.deleteWord(local)
                        }
                    }

                    cloudDeck.words.forEach { cloudWord ->
                        val localWord = localByWordCloudId[cloudWord.cloudId]
                        if (localWord != null) {
                            repo.updateWord(localWord.copy(
                                english = cloudWord.english,
                                russian = cloudWord.russian,
                                transcription = cloudWord.transcription,
                                example = cloudWord.example,
                                exampleTranslation = cloudWord.exampleTranslation,
                                imageUri = cloudWord.imageUri
                            ))
                        } else {
                            repo.insertWord(cloudWord.copy(deckId = deckId))
                        }
                    }
                }

                localDecks.filter { it.cloudId.isBlank() }.forEach { local ->
                    val words = repo.getWordsOnce(local.id)
                    val result = FirestoreService.saveUserDeckAndGetWordIds(userId, local, words)
                    if (result != null) {
                        val (deckCloudId, newWordIds) = result
                        repo.updateDeck(local.copy(cloudId = deckCloudId))
                        newWordIds.forEach { (localWordId, wordCloudId) ->
                            val word = repo.getWordById(localWordId)
                            if (word != null) repo.updateWord(word.copy(cloudId = wordCloudId))
                        }
                    }
                }

                val totalSynced = repo.getAllDecksOnce().size
                if (totalSynced > 0) {
                    com.lango.app.notifications.NotificationHelper.showSyncComplete(
                        getApplication(), totalSynced
                    )
                }
            } catch (_: Exception) {}
        } finally { syncMutex.unlock() }
    }

    fun createDeck(title: String, description: String, emoji: String = "📚", colorIndex: Int = 0) =
        viewModelScope.launch {
            val deckId = repo.insertDeck(Deck(title = title, description = description, emoji = emoji, colorIndex = colorIndex))
            val userId = auth.currentUser?.uid ?: return@launch
            val deck = repo.getDeckById(deckId) ?: return@launch
            val result = FirestoreService.saveUserDeckAndGetWordIds(userId, deck, emptyList())
            if (result != null) repo.updateDeck(deck.copy(cloudId = result.first))
        }

    fun generateDeckWithAI(
        topic: String,
        count: Int,
        mode: GroqApiService.AiDeckMode,
        generateImages: Boolean,
        onResult: (Boolean) -> Unit
    ) = viewModelScope.launch {
        val result = GroqApiService.generateDeck(topic, count, mode)
        if (result != null) {
            val deckId = repo.insertDeck(Deck(title = result.title, description = result.description, emoji = result.emoji, colorIndex = 0))
            val imageUrls = if (generateImages) {
                result.words.map { w ->
                    async { ImageApiService.searchPhoto(w.english) }
                }.awaitAll()
            } else {
                List(result.words.size) { null }
            }
            val words = result.words.mapIndexed { index, w ->
                Word(
                    deckId = deckId,
                    english = w.english,
                    russian = w.russian,
                    transcription = w.transcription,
                    example = w.example,
                    exampleTranslation = w.exampleTranslation,
                    imageUri = imageUrls[index].orEmpty()
                )
            }
            val insertedWords = words.map { word ->
                val newId = repo.insertWord(word)
                word.copy(id = newId)
            }
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val deck = repo.getDeckById(deckId)
                if (deck != null) {
                    val syncResult = FirestoreService.saveUserDeckAndGetWordIds(userId, deck, insertedWords)
                    if (syncResult != null) {
                        val (deckCloudId, newWordIds) = syncResult
                        repo.updateDeck(deck.copy(cloudId = deckCloudId))
                        newWordIds.forEach { (localWordId, wordCloudId) ->
                            val word = repo.getWordById(localWordId)
                            if (word != null) repo.updateWord(word.copy(cloudId = wordCloudId))
                        }
                    }
                }
            }
            onResult(true)
        } else { onResult(false) }
    }

    fun deleteDeck(deck: Deck) = viewModelScope.launch {
        repo.deleteDeck(deck)
        val userId = auth.currentUser?.uid ?: return@launch
        if (deck.cloudId.isNotBlank()) {
            FirestoreService.deleteUserDeck(userId, deck.cloudId)
        }
    }

    fun pushToCloud() = viewModelScope.launch {
        val userId = auth.currentUser?.uid ?: return@launch
        try {
            val localDecks = repo.getAllDecksOnce()
            localDecks.forEach { deck ->
                val words = repo.getWordsOnce(deck.id)
                val result = FirestoreService.saveUserDeckAndGetWordIds(userId, deck, words)
                if (result != null) {
                    val (deckCloudId, newWordIds) = result
                    if (deck.cloudId.isBlank()) {
                        repo.updateDeck(deck.copy(cloudId = deckCloudId))
                    }
                    newWordIds.forEach { (localWordId, wordCloudId) ->
                        val word = repo.getWordById(localWordId)
                        if (word != null) repo.updateWord(word.copy(cloudId = wordCloudId))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun updateDeck(deck: Deck) = viewModelScope.launch {
        repo.updateDeck(deck)
        val userId = auth.currentUser?.uid ?: return@launch
        val words = repo.getWordsOnce(deck.id)
        val result = FirestoreService.saveUserDeckAndGetWordIds(userId, deck, words)
        if (result != null) {
            val (deckCloudId, newWordIds) = result
            if (deck.cloudId.isBlank()) repo.updateDeck(deck.copy(cloudId = deckCloudId))
            newWordIds.forEach { (localWordId, wordCloudId) ->
                val word = repo.getWordById(localWordId)
                if (word != null) repo.updateWord(word.copy(cloudId = wordCloudId))
            }
        }
    }
}

class DeckDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as LangoApplication).repository
    private val auth = FirebaseAuth.getInstance()

    private val _deckId = MutableStateFlow<Long>(-1)

    val words: StateFlow<List<Word>> = _deckId
        .filter { it != -1L }
        .flatMapLatest { repo.getWordsByDeck(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deck = MutableStateFlow<Deck?>(null)
    val deck: StateFlow<Deck?> = _deck

    private val _publishState = MutableStateFlow<String?>(null)
    val publishState: StateFlow<String?> = _publishState

    fun setDeckId(id: Long) {
        _deckId.value = id
        viewModelScope.launch { _deck.value = repo.getDeckById(id) }
    }

    fun deleteWord(word: Word) = viewModelScope.launch {
        repo.deleteWord(word)
        val user = auth.currentUser ?: return@launch
        val deck = _deck.value ?: return@launch
        val remaining = repo.getWordsOnce(deck.id)
        FirestoreService.saveUserDeckAndGetWordIds(user.uid, deck, remaining)
    }

    fun publishDeck() = viewModelScope.launch {
        val deck = _deck.value ?: return@launch
        val user = auth.currentUser ?: return@launch
        val wordList = words.value
        _publishState.value = "publishing"
        val id = FirestoreService.publishDeck(deck, wordList, user.uid, user.email ?: "")
        _publishState.value = if (id != null) "success" else "error"
    }

    fun resetPublishState() { _publishState.value = null }
}

class TrainingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as LangoApplication).repository

    private val sessionQueue = mutableListOf<Word>()

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _correctCount = MutableStateFlow(0)
    val correctCount: StateFlow<Int> = _correctCount

    private val _cardKey = MutableStateFlow(0)
    val cardKey: StateFlow<Int> = _cardKey

    private val _totalWordsCount = MutableStateFlow(0)
    val totalWordsCount: StateFlow<Int> = _totalWordsCount

    private val _finishedWordsCount = MutableStateFlow(0)
    val finishedWordsCount: StateFlow<Int> = _finishedWordsCount

    private val _deckTitle = MutableStateFlow("")

    fun loadWords(deckId: Long) = viewModelScope.launch {
        val loaded = repo.getTrainingWords(deckId)
        val deck = repo.getDeckById(deckId)
        _deckTitle.value = deck?.title ?: ""
        sessionQueue.clear()
        sessionQueue.addAll(loaded)
        _totalWordsCount.value = loaded.size
        _finishedWordsCount.value = 0
        _words.value = sessionQueue.toList()
        _currentIndex.value = 0
        _isFinished.value = false
        _correctCount.value = 0
        _cardKey.value = 0
    }

    fun rateWord(rating: Rating) = viewModelScope.launch {
        val index = _currentIndex.value
        if (index >= sessionQueue.size) return@launch

        val word = sessionQueue[index]
        val updated = repo.calculateNextReview(word, rating)
        repo.updateWord(updated)

        when (rating) {
            Rating.AGAIN -> {
                sessionQueue.removeAt(index)
                val insertAt = minOf(index + 3, sessionQueue.size)
                sessionQueue.add(insertAt, word)
            }
            Rating.HARD -> {
                sessionQueue.removeAt(index)
                val insertAt = minOf(index + 7, sessionQueue.size)
                sessionQueue.add(insertAt, word)
            }
            Rating.GOOD -> {
                _correctCount.value = _correctCount.value + 1
                _finishedWordsCount.value = _finishedWordsCount.value + 1
                sessionQueue.removeAt(index)
            }
            Rating.EASY -> {
                _correctCount.value = _correctCount.value + 1
                _finishedWordsCount.value = _finishedWordsCount.value + 1
                sessionQueue.removeAt(index)
            }
        }

        _words.value = sessionQueue.toList()
        _cardKey.value = _cardKey.value + 1

        if (sessionQueue.isEmpty()) {
            _isFinished.value = true
            val ctx = getApplication<android.app.Application>()
            com.lango.app.notifications.NotificationHelper.showTrainingResult(
                ctx,
                correct = _correctCount.value,
                total = _totalWordsCount.value,
                deckTitle = _deckTitle.value
            )
        }
    }
}

class CatalogViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as LangoApplication).repository

    private val _decks = MutableStateFlow<List<PublicDeckInfo>>(emptyList())
    val decks: StateFlow<List<PublicDeckInfo>> = _decks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _importState = MutableStateFlow<String?>(null)
    val importState: StateFlow<String?> = _importState

    init { loadDecks() }

    fun loadDecks() = viewModelScope.launch {
        _isLoading.value = true
        _decks.value = FirestoreService.getPublicDecks()
        _isLoading.value = false
    }

    fun importDeck(info: PublicDeckInfo) = viewModelScope.launch {
        _importState.value = "loading"
        if (info.firestoreId.isNotBlank() && repo.getDeckByFirestoreId(info.firestoreId) != null) {
            _importState.value = "exists"
            return@launch
        }
        val words = FirestoreService.getPublicDeckWords(info.firestoreId)
        val deckId = repo.insertDeck(
            Deck(title = info.title, description = info.description, firestoreId = info.firestoreId, emoji = info.emoji, colorIndex = info.colorIndex)
        )
        val insertedWords = words.map { word ->
            val newId = repo.insertWord(word.copy(deckId = deckId))
            word.copy(id = newId, deckId = deckId)
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val deck = repo.getDeckById(deckId)
            if (deck != null) {
                val result = FirestoreService.saveUserDeckAndGetWordIds(userId, deck, insertedWords)
                if (result != null) {
                    val (deckCloudId, newWordIds) = result
                    repo.updateDeck(deck.copy(cloudId = deckCloudId))
                    newWordIds.forEach { (localWordId, wordCloudId) ->
                        val word = repo.getWordById(localWordId)
                        if (word != null) repo.updateWord(word.copy(cloudId = wordCloudId))
                    }
                }
            }
        }
        _importState.value = "success"
    }

    fun resetImportState() { _importState.value = null }
}
