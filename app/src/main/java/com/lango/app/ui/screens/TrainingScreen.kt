package com.lango.app.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.lango.app.domain.model.Rating
import com.lango.app.ui.components.*
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.TrainingViewModel
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun TrainingScreen(
    deckId: Long,
    viewModel: TrainingViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val words by viewModel.words.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val finishedWordsCount by viewModel.finishedWordsCount.collectAsState()
    val totalWordsCount by viewModel.totalWordsCount.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()
    val correctCount by viewModel.correctCount.collectAsState()
    val cardKey by viewModel.cardKey.collectAsState()

    var isFlipped by remember(cardKey) { mutableStateOf(false) }
    var prevFlipped by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val t = TextToSpeech(context) {}
        tts = t
        onDispose { t.shutdown() }
    }

    val soundPool = remember {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_GAME)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            .let { attrs ->
                android.media.SoundPool.Builder()
                    .setMaxStreams(2)
                    .setAudioAttributes(attrs)
                    .build()
            }
    }
    val soundCorrect = remember { soundPool.load(context, com.lango.app.R.raw.sound_correct, 1) }
    val soundWrong   = remember { soundPool.load(context, com.lango.app.R.raw.sound_wrong, 1) }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    fun playSound(isCorrect: Boolean) {
        if (isCorrect) soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
        else           soundPool.play(soundWrong,   1f, 1f, 0, 0, 1f)
    }

    fun speakEn(text: String) {
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }
    fun speakRu(text: String) {
        tts?.language = Locale("ru")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    LaunchedEffect(deckId) { viewModel.loadWords(deckId) }

    LaunchedEffect(currentIndex, words) {
        val nextWord = words.getOrNull(currentIndex + 1)
        if (nextWord != null && nextWord.imageUri.isNotEmpty()) {
            val request = coil.request.ImageRequest.Builder(context)
                .data(nextWord.imageUri)
                .build()
            coil.ImageLoader(context).enqueue(request)
        }
    }

    if (showExitDialog) {
        LangoAlertDialog(
            title = "Завершить тренировку?",
            message = "Прогресс этой сессии будет сохранён. Выйти?",
            confirmText = "Завершить",
            onConfirm = { onBack() },
            onDismiss = { showExitDialog = false }
        )
    }

    if (isFinished) {
        FinishedScreen(total = totalWordsCount, correct = correctCount, onBack = onBack)
        return
    }

    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize().background(DarkBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val word = words.getOrNull(currentIndex) ?: return

    Box(Modifier.fillMaxSize().background(DarkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showExitDialog = true }) {
                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                }
                Text(
                    "${minOf(finishedWordsCount + 1, totalWordsCount)} / $totalWordsCount",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(8.dp))
            LangoProgressBar(
                progress = if (totalWordsCount > 0) finishedWordsCount.toFloat() / totalWordsCount else 0f,
                modifier = Modifier.fillMaxWidth(),
                height = 6.dp,
                color = Primary,
                trackColor = DarkCard
            )

            Spacer(Modifier.height(24.dp))

            Text(
                if (!isFlipped) "Нажмите на карточку, чтобы перевернуть"
                else "Оцените, как вы знаете слово",
                color = TextHint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = cardKey to word,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280)) +
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth / 4 },
                                animationSpec = tween(280, easing = EaseOutCubic)
                            )).togetherWith(
                        fadeOut(animationSpec = tween(150))
                    )
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "cardAnim"
            ) { (key, currentWord) ->
                val displayFlipped = if (key == cardKey) isFlipped else prevFlipped
                FlipCard(
                    isFlipped = displayFlipped,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isFlipped = !isFlipped },
                    frontContent = {
                        CardFace(
                            title = currentWord.english,
                            subtitle = currentWord.transcription,
                            extraText = currentWord.example.ifEmpty { null },
                            extraTextTranslation = null,
                            imageUri = currentWord.imageUri,
                            onSpeak = { speakEn(currentWord.english) },
                            isFront = true
                        )
                    },
                    backContent = {
                        CardFace(
                            title = currentWord.russian,
                            subtitle = currentWord.transcription,
                            extraText = currentWord.example.ifEmpty { null },
                            extraTextTranslation = currentWord.exampleTranslation.ifEmpty { null },
                            imageUri = currentWord.imageUri,
                            onSpeak = { speakRu(currentWord.russian) },
                            isFront = false
                        )
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = isFlipped) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RatingButton("❌\nНе знал", ColorError, Modifier.weight(1f)) {
                            prevFlipped = isFlipped
                            playSound(isCorrect = false)
                            viewModel.rateWord(Rating.AGAIN)
                        }
                        RatingButton("😐\nС трудом", ColorWarning, Modifier.weight(1f)) {
                            prevFlipped = isFlipped
                            playSound(isCorrect = false)
                            viewModel.rateWord(Rating.HARD)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RatingButton("✅\nЗнал", ColorSuccess, Modifier.weight(1f)) {
                            prevFlipped = isFlipped
                            playSound(isCorrect = true)
                            viewModel.rateWord(Rating.GOOD)
                        }
                        RatingButton("🔥\nЛегко", Primary, Modifier.weight(1f)) {
                            prevFlipped = isFlipped
                            playSound(isCorrect = true)
                            viewModel.rateWord(Rating.EASY)
                        }
                    }
                }
            }

            if (!isFlipped) Spacer(Modifier.height(80.dp))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CardFace(
    title: String,
    subtitle: String,
    extraText: String?,
    extraTextTranslation: String?,
    imageUri: String,
    onSpeak: () -> Unit,
    isFront: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(DarkCard),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                if (isFront) "EN" else "RU",
                color = if (isFront) Primary else Secondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            if (isFront && imageUri.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, color = TextSecondary, fontSize = 16.sp)
            }
            if (extraText != null) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = DarkSurface)
                Spacer(Modifier.height(12.dp))
                Text(
                    extraText,
                    color = if (isFront) TextSecondary else TextHint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
                if (!isFront && extraTextTranslation != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        extraTextTranslation,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            IconButton(
                onClick = onSpeak,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isFront) Primary.copy(0.2f) else Secondary.copy(0.2f))
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    "Произнести",
                    tint = if (isFront) Primary else Secondary
                )
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun FinishedScreen(total: Int, correct: Int, onBack: () -> Unit) {
    val pct = if (total > 0) (correct * 100 / total) else 0
    Box(
        Modifier.fillMaxSize().background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(if (pct >= 70) "🎉" else "💪", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Тренировка завершена!",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Правильно: $correct из $total ($pct%)",
                color = TextSecondary,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(32.dp))
            GradientButton("Готово", onBack, Modifier.fillMaxWidth())
        }
    }
}
