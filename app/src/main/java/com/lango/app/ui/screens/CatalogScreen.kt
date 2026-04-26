package com.lango.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.lango.app.data.remote.FirestoreService
import com.lango.app.data.remote.PublicDeckInfo
import com.lango.app.domain.model.Word
import com.lango.app.ui.components.*
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val decks by viewModel.decks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val importState by viewModel.importState.collectAsState()
    var deckToImport by remember { mutableStateOf<PublicDeckInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var previewDeck by remember { mutableStateOf<PublicDeckInfo?>(null) }
    var previewWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var previewLoading by remember { mutableStateOf(false) }

    val filteredDecks = remember(decks, searchQuery) {
        if (searchQuery.isBlank()) decks
        else decks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.userEmail.contains(searchQuery, ignoreCase = true)
        }
    }

    deckToImport?.let { info ->
        LangoAlertDialog("Добавить колоду?",
            "«${info.title}» (${info.wordCount} слов) будет добавлена в ваши колоды.",
            "Добавить", confirmColor = Primary,
            onConfirm = { viewModel.importDeck(info); deckToImport = null },
            onDismiss = { deckToImport = null })
    }

    previewDeck?.let { info ->
        AlertDialog(
            onDismissRequest = { previewDeck = null; previewWords = emptyList(); previewLoading = false },
            containerColor = DarkCard,
            titleContentColor = TextPrimary,
            title = { Text(info.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (info.description.isNotBlank()) {
                        Text(info.description, color = TextSecondary, fontSize = 13.sp)
                    }
                    if (previewLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                            Text("Загружаю слова...", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        val list = previewWords.take(20)
                        if (list.isEmpty()) {
                            Text("Не удалось загрузить слова.", color = TextHint, fontSize = 12.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                list.forEach { w ->
                                    Text("${w.english} — ${w.russian}", color = TextPrimary, fontSize = 13.sp)
                                }
                            }
                            if (previewWords.size > 20) {
                                Text("…и ещё ${previewWords.size - 20}", color = TextHint, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { deckToImport = info; previewDeck = null }) {
                    Text("Добавить", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewDeck = null; previewWords = emptyList(); previewLoading = false }) {
                    Text("Закрыть", color = TextSecondary)
                }
            }
        )
    }

    LaunchedEffect(importState) {
        if (importState == "success") {
            kotlinx.coroutines.delay(2000); viewModel.resetImportState()
        }
    }
    LaunchedEffect(importState) {
        if (importState == "exists") {
            kotlinx.coroutines.delay(2000); viewModel.resetImportState()
        }
    }

    LaunchedEffect(previewDeck?.firestoreId) {
        val id = previewDeck?.firestoreId ?: return@LaunchedEffect
        previewLoading = true
        previewWords = FirestoreService.getPublicDeckWords(id)
        previewLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBg).statusBarsPadding()
        .padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Каталог", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("Готовые колоды для изучения", color = TextSecondary, fontSize = 13.sp)
            }
            IconButton(onClick = { viewModel.loadDecks() }) {
                Icon(Icons.Default.Refresh, null, tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Поиск колод...", color = TextHint) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary) } }
            } else null,
            shape = RoundedCornerShape(14.dp),
            colors = outlinedFieldColors(),
            singleLine = true
        )

        if (importState == "success") {
            Spacer(Modifier.height(8.dp))
            Text("✅ Колода добавлена!", color = ColorSuccess, fontSize = 13.sp)
        }
        if (importState == "exists") {
            Spacer(Modifier.height(8.dp))
            Text("⚠️ Эта колода уже добавлена", color = ColorWarning, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            filteredDecks.isEmpty() && decks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("🌍", "Каталог пуст", "Публичных колод пока нет. Опубликуй первую!")
            }
            filteredDecks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("🔍", "Ничего не найдено", "Попробуй другой запрос")
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)) {
                items(filteredDecks.size) { i ->
                    PublicDeckCard(
                        deck = filteredDecks[i],
                        onImport = { deckToImport = filteredDecks[i] },
                        onPreview = { previewDeck = filteredDecks[i] }
                    )
                }
            }
        }
    }
}

private val catalogEmojis = listOf(
    "✈️","🌍","📚","💻","🧠","🎧","🏋️","🍜","💬","🎯","🧳","🗺️","🎓","⚡","💼"
)

@Composable
private fun PublicDeckCard(deck: PublicDeckInfo, onImport: () -> Unit, onPreview: () -> Unit) {
    val gradientIndex = ((deck.title.hashCode() % (DeckColors.size - 1)).let { if (it < 0) it + (DeckColors.size - 1) else it }) + 1
    val gradient = DeckColors.getOrElse(gradientIndex) { DeckColors[0] }
    val tint = Brush.horizontalGradient(
        DeckColorStops.getOrElse(gradientIndex) { DeckColorStops[0] }.map { it.copy(alpha = 0.16f) }
    )
    val emoji = deck.emoji.ifBlank { catalogEmojis[(deck.title.hashCode().let { if (it < 0) -it else it }) % catalogEmojis.size] }

    Card(
        onClick = onPreview,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(1.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCard)
                    .background(tint)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(deck.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(deck.userEmail, color = TextHint, fontSize = 11.sp)
                        Text("${deck.wordCount} слов", color = TextSecondary, fontSize = 12.sp)
                    }
                    Button(onClick = onImport, shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary.copy(alpha = 0.18f),
                            contentColor = PrimaryLight
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("+ Добавить", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
