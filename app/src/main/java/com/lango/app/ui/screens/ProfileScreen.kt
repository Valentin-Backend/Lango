package com.lango.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.lango.app.ui.components.GlassCard
import com.lango.app.ui.components.LangoAlertDialog
import com.lango.app.ui.theme.*
import com.lango.app.viewmodel.AuthViewModel
import com.lango.app.viewmodel.DecksViewModel

private val avatarEmojis = listOf(
    "😊","🚀","⚡","🌸","🎯","🔥","🌊","🎸","🦊","🐉",
    "💎","🌙","⭐","🏆","🎭","🦁","🎓","💻","🎨","🏋️"
)

@Composable
fun ProfileScreen(authViewModel: AuthViewModel, decksViewModel: DecksViewModel) {
    val decks by decksViewModel.decks.collectAsState()
    val user = authViewModel.currentUser
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("lango_profile", android.content.Context.MODE_PRIVATE) }
    var selectedAvatar by remember { mutableStateOf(prefs.getString("avatar", "😊") ?: "😊") }
    var displayName by remember { mutableStateOf(prefs.getString("displayName", "") ?: "") }
    var bio by remember { mutableStateOf(prefs.getString("bio", "") ?: "") }

    val totalWords = decks.sumOf { it.wordCount }
    val learnedWords = decks.sumOf { it.learnedCount }
    val totalDue = decks.sumOf { it.dueCount }

    if (showLogoutDialog) {
        LangoAlertDialog("Выйти из аккаунта?", "Локальные данные сохранятся. Вы сможете войти снова.",
            "Выйти", onConfirm = {
                decksViewModel.pushToCloud()
                authViewModel.logout()
            }, onDismiss = { showLogoutDialog = false })
    }
    if (showEditProfile) {
        var newAvatar by remember { mutableStateOf(selectedAvatar) }
        var newName by remember { mutableStateOf(displayName) }
        var newBio by remember { mutableStateOf(bio) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            containerColor = DarkCard, titleContentColor = TextPrimary,
            title = { Text("Профиль", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Аватар", color = TextSecondary, fontSize = 12.sp)
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(avatarEmojis.size) { i ->
                            val emoji = avatarEmojis[i]
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                                    .background(if (newAvatar == emoji) Primary.copy(0.2f) else DarkSurface)
                                    .then(if (newAvatar == emoji) Modifier.border(2.dp, Primary, CircleShape) else Modifier)
                                    .clickable { newAvatar = emoji },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 24.sp) }
                        }
                    }
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(24) },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it.take(80) },
                        label = { Text("О себе") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedFieldColors(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedAvatar = newAvatar
                    displayName = newName.trim()
                    bio = newBio.trim()
                    prefs.edit()
                        .putString("avatar", selectedAvatar)
                        .putString("displayName", displayName)
                        .putString("bio", bio)
                        .apply()
                    showEditProfile = false
                }) { Text("Сохранить", color = Primary, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Отмена", color = TextSecondary) }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBg).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Профиль", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Spacer(Modifier.height(24.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape)
                    .background(Primary.copy(0.2f))
                    .border(2.dp, Primary.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(selectedAvatar, fontSize = 42.sp)
            }
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(BrightPurple)
                    .clickable { showEditProfile = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            displayName.takeIf { it.isNotBlank() }
                ?: user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore("@") ?: "Гость",
            color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp
        )
        if (bio.isNotBlank()) {
            Text(bio, color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        if (user?.email != null) {
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .background(Primary.copy(0.15f)).padding(horizontal = 14.dp, vertical = 5.dp)) {
                Text(user.email ?: "", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(6.dp))

        Spacer(Modifier.height(24.dp))

        Text("Статистика", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(0.5f), letterSpacing = 0.5.sp,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStatCard("${decks.size}", "Колод", Modifier.weight(1f))
            ProfileStatCard("$totalWords", "Слов", Modifier.weight(1f))
            ProfileStatCard("$learnedWords", "Изучено", Modifier.weight(1f))
        }

        if (totalDue > 0) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), cornerRadius = 14.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = ColorWarning, modifier = Modifier.size(20.dp))
                    Column {
                        Text("К повторению сегодня", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("$totalDue слов ждут повторения", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Достижения", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(0.5f), letterSpacing = 0.5.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AchievementBadge("🎯", "Первое слово", totalWords >= 1, Modifier.weight(1f))
            AchievementBadge("📚", "10 слов", totalWords >= 10, Modifier.weight(1f))
            AchievementBadge("🏆", "50 слов", totalWords >= 50, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AchievementBadge("🌟", "100 слов", totalWords >= 100, Modifier.weight(1f))
            AchievementBadge("🎓", "1 изучено", learnedWords >= 1, Modifier.weight(1f))
            AchievementBadge("🔥", "Серия 7д", false, Modifier.weight(1f))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorError.copy(0.12f))
        ) {
            Icon(Icons.Filled.Logout, null, tint = ColorError, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (user == null) "Войти в аккаунт" else "Выйти из аккаунта",
                color = ColorError, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ProfileStatCard(value: String, label: String, modifier: Modifier) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp, padding = 14.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AchievementBadge(icon: String, title: String, unlocked: Boolean, modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(if (unlocked) Primary.copy(0.12f) else DarkCard)
            .then(if (unlocked) Modifier.border(1.dp, Primary.copy(0.3f), RoundedCornerShape(14.dp)) else Modifier)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 22.sp, color = if (unlocked) Color.Unspecified else Color.Unspecified.copy(alpha = 0.3f))
            Text(title, color = if (unlocked) TextPrimary else TextHint, fontSize = 9.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
            if (unlocked) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ColorSuccess))
            }
        }
    }
}
