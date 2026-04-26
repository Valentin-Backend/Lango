package com.lango.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import com.lango.app.domain.model.Deck
import com.lango.app.ui.components.*
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.DecksViewModel

@Composable
fun DecksScreen(viewModel: DecksViewModel, onDeckClick: (Long) -> Unit) {
    val decks by viewModel.decks.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var deckToEditStyle by remember { mutableStateOf<Deck?>(null) }
    var deckToRename by remember { mutableStateOf<Deck?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("📚") }
    var newColorIndex by remember { mutableStateOf(0) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    var showAiDialog by remember { mutableStateOf(false) }
    var aiTopic by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var aiCount by remember { mutableStateOf(20) }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiMode by remember { mutableStateOf(com.lango.app.data.remote.GroqApiService.AiDeckMode.PHRASES) }
    var aiGenerateImages by remember { mutableStateOf(false) }

    deckToDelete?.let { deck ->
        LangoAlertDialog("Удалить колоду?", "«${deck.title}» и все слова будут удалены.",
            "Удалить", onConfirm = { viewModel.deleteDeck(deck); deckToDelete = null },
            onDismiss = { deckToDelete = null })
    }
    deckToEditStyle?.let { deck ->
        EmojiColorPickerDialog(deck.emoji, deck.colorIndex,
            onConfirm = { e, c -> viewModel.updateDeck(deck.copy(emoji = e, colorIndex = c)); deckToEditStyle = null },
            onDismiss = { deckToEditStyle = null })
    }
    deckToRename?.let { deck ->
        var title by remember(deck.id) { mutableStateOf(deck.title) }
        var desc by remember(deck.id) { mutableStateOf(deck.description) }
        AlertDialog(
            onDismissRequest = { deckToRename = null },
            containerColor = DarkCard,
            titleContentColor = TextPrimary,
            title = { Text("Переименовать", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = title.trim()
                        if (t.isNotBlank()) {
                            viewModel.updateDeck(deck.copy(title = t, description = desc.trim()))
                            deckToRename = null
                        }
                    }
                ) { Text("Сохранить", color = Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { deckToRename = null }) { Text("Отмена", color = TextSecondary) }
            }
        )
    }
    if (showEmojiPicker) {
        EmojiColorPickerDialog(newEmoji, newColorIndex,
            onConfirm = { e, c -> newEmoji = e; newColorIndex = c; showEmojiPicker = false },
            onDismiss = { showEmojiPicker = false })
    }
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newTitle = ""; newDesc = "" },
            containerColor = DarkCard, titleContentColor = TextPrimary,
            title = { Text("Новая колода", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                .background(DeckColors.getOrElse(newColorIndex) { DeckColors[0] })
                                .clickable { showEmojiPicker = true },
                            contentAlignment = Alignment.Center
                        ) { Text(newEmoji, fontSize = 24.sp) }
                        Text("Нажми для смены оформления", color = TextSecondary, fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Название") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        singleLine = true)
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Описание (необязательно)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(), maxLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) {
                        viewModel.createDeck(newTitle.trim(), newDesc.trim(), newEmoji, newColorIndex)
                        showCreateDialog = false; newTitle = ""; newDesc = ""; newEmoji = "📚"; newColorIndex = 0
                    }
                }) { Text("Создать", color = Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newTitle = ""; newDesc = "" }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) { showAiDialog = false; aiTopic = "" } },
            containerColor = DarkCard, titleContentColor = TextPrimary,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Secondary, modifier = Modifier.size(20.dp))
                    Text("Создать колоду с AI", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (aiMode == com.lango.app.data.remote.GroqApiService.AiDeckMode.WORDS)
                            "Режим: слова + пример предложения."
                        else
                            "Режим: фразы/предложения.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = aiMode == com.lango.app.data.remote.GroqApiService.AiDeckMode.WORDS,
                            onClick = { aiMode = com.lango.app.data.remote.GroqApiService.AiDeckMode.WORDS },
                            label = { Text("Слова") },
                            leadingIcon = {
                                if (aiMode == com.lango.app.data.remote.GroqApiService.AiDeckMode.WORDS)
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                else null
                            }
                        )
                        FilterChip(
                            selected = aiMode == com.lango.app.data.remote.GroqApiService.AiDeckMode.PHRASES,
                            onClick = { aiMode = com.lango.app.data.remote.GroqApiService.AiDeckMode.PHRASES },
                            label = { Text("Фразы") },
                            leadingIcon = {
                                if (aiMode == com.lango.app.data.remote.GroqApiService.AiDeckMode.PHRASES)
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                else null
                            }
                        )
                    }
                    OutlinedTextField(
                        value = aiTopic,
                        onValueChange = { aiTopic = it },
                        label = { Text("Тема (напр. «Спорт», «Технологии»)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true,
                        enabled = !isGenerating
                    )
                    Column {
                        Text("Количество карточек: $aiCount", color = TextSecondary, fontSize = 12.sp)
                        Slider(
                            value = aiCount.toFloat(),
                            onValueChange = { aiCount = it.toInt().coerceIn(10, 50) },
                            valueRange = 10f..50f,
                            steps = 3
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = aiGenerateImages,
                            onCheckedChange = { aiGenerateImages = it }
                        )
                        Text(
                            "Подбирать фото к словам",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                    if (aiError != null) {
                        Text(aiError!!, color = ColorError, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (aiTopic.isNotBlank()) {
                            isGenerating = true
                            aiError = null
                            viewModel.generateDeckWithAI(aiTopic.trim(), aiCount, aiMode, aiGenerateImages) { success ->
                                isGenerating = false
                                if (success) { showAiDialog = false; aiTopic = "" }
                                else aiError = "Не удалось сгенерировать колоду. Попробуй другую тему или меньше карточек."
                            }
                        }
                    }, enabled = aiTopic.isNotBlank() && !isGenerating
                ) {
                    if (isGenerating) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(14.dp), color = Primary, strokeWidth = 2.dp)
                            Text("Генерирую...", color = Primary)
                        }
                    } else {
                        Text("Создать", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isGenerating) { showAiDialog = false; aiTopic = "" } }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Мои колоды", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
                    if (decks.isNotEmpty())
                        Text("${decks.size} колод · ${decks.sumOf { it.wordCount }} слов",
                            color = TextSecondary, fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(PrimaryGradient),
                        contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Secondary),
                        contentAlignment = Alignment.Center) {
                        IconButton(onClick = { showAiDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (decks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("📚", "Нет колод", "Нажмите + чтобы создать первую колоду")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)) {
                    itemsIndexed(decks) { _, deck ->
                        DeckCard(deck, { onDeckClick(deck.id) }, { deckToDelete = deck },
                            onEditStyle = { deckToEditStyle = deck },
                            onRename = { deckToRename = deck })
                    }
                }
            }
        }
    }
}

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Primary, unfocusedBorderColor = TextHint,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Primary,
    focusedLabelColor = Primary, unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
)
