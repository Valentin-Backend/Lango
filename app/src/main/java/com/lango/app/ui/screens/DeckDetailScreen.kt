package com.lango.app.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lango.app.domain.model.WordLevel
import com.lango.app.ui.components.*
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.DeckDetailViewModel
import com.lango.app.EditWordActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deckId: Long,
    viewModel: DeckDetailViewModel,
    onBack: () -> Unit,
    onStartTraining: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val deck by viewModel.deck.collectAsState()
    val words by viewModel.words.collectAsState()
    val publishState by viewModel.publishState.collectAsState()
    var showPublishDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, deckId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setDeckId(deckId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showPublishDialog) {
        LangoAlertDialog(
            title = "Опубликовать колоду?",
            message = "Колода «${deck?.title}» будет видна всем пользователям в каталоге.",
            confirmText = "Опубликовать",
            confirmColor = Primary,
            onConfirm = { viewModel.publishDeck(); showPublishDialog = false },
            onDismiss = { showPublishDialog = false }
        )
    }

    LaunchedEffect(publishState) {
        if (publishState == "success" || publishState == "error") {
            kotlinx.coroutines.delay(2000)
            viewModel.resetPublishState()
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = { Text(deck?.title ?: "", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showPublishDialog = true }) {
                        Icon(Icons.Default.Share, null, tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(context, EditWordActivity::class.java).apply {
                        putExtra("deck_id", deckId)
                    }
                    context.startActivity(intent)
                },
                containerColor = Primary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Добавить слово")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip(
                        icon = Icons.Default.MenuBook,
                        value = words.size.toString(),
                        label = "слов",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Default.CheckCircle,
                        value = words.count { it.level == WordLevel.KNOWN }.toString(),
                        label = "изучено",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        icon = Icons.Default.Schedule,
                        value = words.count {
                            it.nextReviewDate <= System.currentTimeMillis() && it.level != WordLevel.NEW
                        }.toString(),
                        label = "к повторению",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                GradientButton(
                    text = "Начать тренировку",
                    onClick = onStartTraining,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = words.isNotEmpty()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (publishState != null) {
                item {
                    val msg = when (publishState) {
                        "publishing" -> "⏳ Публикация..."
                        "success" -> "✅ Опубликовано!"
                        "error" -> "❌ Ошибка публикации"
                        else -> ""
                    }
                    Text(msg, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }

            item {
                Text(
                    "Слова (${words.size})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            if (words.isEmpty()) {
                item {
                    EmptyState(
                        icon = "📝",
                        title = "Нет слов",
                        subtitle = "Нажмите + чтобы добавить первое слово"
                    )
                }
            } else {
                items(words.size) { i ->
                    val word = words[i]
                    WordListItem(
                        word = word,
                        onClick = {
                            val intent = Intent(context, EditWordActivity::class.java).apply {
                                putExtra("deck_id", deckId)
                                putExtra("word_id", word.id)
                                putExtra("word_english", word.english)
                                putExtra("word_russian", word.russian)
                                putExtra("word_transcription", word.transcription)
                                putExtra("word_example", word.example)
                                putExtra("word_image_uri", word.imageUri)
                                putExtra("word_cloud_id", word.cloudId)
                            }
                            context.startActivity(intent)
                        },
                        onDelete = { viewModel.deleteWord(word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}
