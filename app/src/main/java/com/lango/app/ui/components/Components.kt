package com.lango.app.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.lango.app.domain.model.*
import com.lango.app.ui.theme.*

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) PrimaryGradient else Brush.horizontalGradient(listOf(TextHint, TextHint)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun LangoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = Primary,
    trackColor: Color = DarkSurface
) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .height(height)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .background(color)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassGradient)
                .padding(padding)
        ) {
            content()
        }
    }
}

@Composable
fun LangoAlertDialog(
    title: String,
    message: String,
    confirmText: String = "Подтвердить",
    dismissText: String = "Отмена",
    confirmColor: Color = ColorError,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Text(message, fontSize = 14.sp, lineHeight = 20.sp)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = confirmColor, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText, color = TextSecondary)
            }
        }
    )
}

@Composable
fun DeckCard(
    deck: Deck,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEditStyle: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val colorIndex = deck.colorIndex.coerceIn(0, DeckColors.lastIndex)
    val deckBrush = DeckColors[colorIndex]
    val tintBrush = Brush.horizontalGradient(
        DeckColorStops[colorIndex].map { it.copy(alpha = 0.16f) }
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(deckBrush)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
                    .background(tintBrush)
                    .padding(16.dp)
            ) {
                Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (deck.emoji.isNotBlank()) deck.emoji else deck.title.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = DarkCardAlt
                        ) {
                            if (onRename != null) {
                                DropdownMenuItem(
                                    text = { Text("Переименовать", color = TextPrimary) },
                                    onClick = { showMenu = false; onRename() },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) }
                                )
                            }
                            if (onEditStyle != null) {
                                DropdownMenuItem(
                                    text = { Text("Оформление", color = TextPrimary) },
                                    onClick = { showMenu = false; onEditStyle() },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, tint = Primary) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Удалить", color = ColorError) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ColorError) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    deck.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (deck.description.isNotEmpty()) {
                    Text(
                        deck.description,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(12.dp))

                val progress = if (deck.wordCount > 0) deck.learnedCount.toFloat() / deck.wordCount else 0f
                LangoProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 4.dp,
                    color = Primary,
                    trackColor = DarkSurface
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${deck.wordCount} слов", color = TextSecondary, fontSize = 11.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer(scaleX = 0.9f, scaleY = 0.9f)
                    ) {
                        if (deck.dueCount > 0) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ColorError.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${deck.dueCount} к повторению",
                                color = ColorError.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ColorSuccess.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${deck.learnedCount} изучено",
                                color = ColorSuccess.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

private val deckEmojis = listOf(
    "📚","⚡","✈️","💼","😊","🍕","🎧","🌍","🎯","🔥"
)

@Composable
fun EmojiColorPickerDialog(
    initialEmoji: String,
    initialColorIndex: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedEmoji by remember { mutableStateOf(initialEmoji) }
    var selectedColorIndex by remember { mutableStateOf(initialColorIndex.coerceIn(0, DeckColors.lastIndex)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        title = { Text("Оформление колоды", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Выбери эмодзи и цвет фона для карточки.", color = TextSecondary, fontSize = 13.sp)

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    deckEmojis.forEach { emoji ->
                        val selected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Primary.copy(0.2f) else DarkSurface)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Primary else DarkSurface,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckColors.forEachIndexed { index, brush ->
                        val selected = index == selectedColorIndex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(brush)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) Color.White else Color.White.copy(0.3f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedEmoji, selectedColorIndex) }) {
                Text("Готово", color = Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
fun WordListItem(
    word: Word,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        LangoAlertDialog(
            title = "Удалить слово?",
            message = "«${word.english}» будет удалено из колоды. Это действие нельзя отменить.",
            confirmText = "Удалить",
            onConfirm = { showDeleteDialog = false; onDelete() },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (word.level) {
                            WordLevel.NEW -> ColorNew
                            WordLevel.LEARNING -> ColorWarning
                            WordLevel.KNOWN -> ColorSuccess
                        }
                    )
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(word.english, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(word.russian, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextHint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun FlipCard(
    frontContent: @Composable () -> Unit,
    backContent: @Composable () -> Unit,
    isFlipped: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "flip"
    )

    val showFront = rotation < 90f || rotation > 270f

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 14f * density
        }
    ) {
        if (showFront) {
            Box(Modifier.fillMaxSize()) { frontContent() }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) { backContent() }
        }
    }
}

@Composable
fun LevelBadge(level: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(level, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TextSecondary, fontSize = 14.sp)
    }
}
