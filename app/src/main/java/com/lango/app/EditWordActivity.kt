package com.lango.app

import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.lifecycleScope
import com.lango.app.data.local.LangoDatabase
import com.lango.app.data.remote.AiCardResult
import com.lango.app.data.remote.GroqApiService
import com.lango.app.data.remote.ImageApiService
import com.lango.app.data.remote.LevelExample
import com.lango.app.data.repository.LangoRepository
import com.lango.app.domain.model.Word
import com.lango.app.ui.components.LangoAlertDialog
import com.lango.app.ui.screens.outlinedFieldColors
import com.lango.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

class EditWordActivity : ComponentActivity() {

    private lateinit var repo: LangoRepository
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deckId = intent.getLongExtra("deck_id", -1L)
        val wordId = intent.getLongExtra("word_id", -1L)
        val initialEnglish = intent.getStringExtra("word_english") ?: ""
        val initialRussian = intent.getStringExtra("word_russian") ?: ""
        val initialTranscription = intent.getStringExtra("word_transcription") ?: ""
        val initialExample = intent.getStringExtra("word_example") ?: ""
        val initialImageUri = intent.getStringExtra("word_image_uri") ?: ""
        val wordCloudId = intent.getStringExtra("word_cloud_id") ?: ""

        val db = LangoDatabase.getInstance(applicationContext)
        repo = LangoRepository(db.deckDao(), db.wordDao())

        tts = TextToSpeech(this) {}

        setContent {
            LangoTheme {
                EditWordScreen(
                    deckId = deckId,
                    wordId = wordId,
                    wordCloudId = wordCloudId,
                    initialEnglish = initialEnglish,
                    initialRussian = initialRussian,
                    initialTranscription = initialTranscription,
                    initialExample = initialExample,
                    initialImageUri = initialImageUri,
                    onSave = { word ->
                        lifecycleScope.launch {
                            if (wordId == -1L) repo.insertWord(word)
                            else repo.updateWord(word)
                            runOnUiThread {
                                Toast.makeText(
                                    this@EditWordActivity,
                                    if (wordId == -1L) "Слово добавлено!" else "Слово обновлено!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    },
                    onBack = { finish() },
                    onSpeak = { text, lang ->
                        tts?.language = if (lang == "en") Locale.ENGLISH else Locale("ru")
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWordScreen(
    deckId: Long,
    wordId: Long,
    wordCloudId: String,
    initialEnglish: String,
    initialRussian: String,
    initialTranscription: String,
    initialExample: String,
    initialImageUri: String,
    onSave: (Word) -> Unit,
    onBack: () -> Unit,
    onSpeak: (String, String) -> Unit
) {
    val isEdit = wordId != -1L

    val context = LocalContext.current
    var english by rememberSaveable { mutableStateOf(initialEnglish) }
    var russian by rememberSaveable { mutableStateOf(initialRussian) }
    var transcription by rememberSaveable { mutableStateOf(initialTranscription) }
    var example by rememberSaveable { mutableStateOf(initialExample) }
    var exampleTranslation by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf(initialImageUri) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var isImageGenerating by rememberSaveable { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf<AiCardResult?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                imageUri = file.absolutePath
            } catch (e: Exception) {
                imageUri = uri.toString()
            }
        }
    }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    if (showDiscardDialog) {
        LangoAlertDialog(
            title = "Отменить изменения?",
            message = "Несохранённые данные будут потеряны.",
            confirmText = "Отменить",
            onConfirm = { onBack() },
            onDismiss = { showDiscardDialog = false }
        )
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) "Редактировать слово" else "Новое слово",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (english.isNotBlank() || russian.isNotBlank()) showDiscardDialog = true
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = english,
                    onValueChange = { english = it; aiResult = null },
                    label = { Text("Слово на английском") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (english.isNotBlank()) onSpeak(english, "en") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary.copy(0.15f))
                ) {
                    Icon(Icons.Default.VolumeUp, "Произнести", tint = Primary)
                }
            }

            Button(
                onClick = {
                    if (english.isNotBlank()) {
                        isLoading = true
                        scope.launch {
                            aiResult = GroqApiService.generateCardData(english.trim())
                            aiResult?.let {
                                russian = it.translation
                                transcription = it.transcription
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = english.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Secondary.copy(0.15f),
                    contentColor = Secondary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Secondary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isLoading) "Генерирую..." else "✨ Сгенерировать через AI",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (aiResult != null && !isLoading) {
                Text("Выберите пример предложения:", color = TextSecondary, fontSize = 13.sp)
                aiResult!!.examples.forEach { ex ->
                    ExampleCard(ex = ex, onSelect = { en, ru -> example = en; exampleTranslation = ru })
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = russian,
                    onValueChange = { russian = it },
                    label = { Text("Перевод на русский") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = outlinedFieldColors(),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (russian.isNotBlank()) onSpeak(russian, "ru") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Secondary.copy(0.15f))
                ) {
                    Icon(Icons.Default.VolumeUp, "Произнести", tint = Secondary)
                }
            }

            OutlinedTextField(
                value = transcription,
                onValueChange = { transcription = it },
                label = { Text("Транскрипция (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedFieldColors(),
                singleLine = true
            )

            OutlinedTextField(
                value = example,
                onValueChange = { example = it },
                label = { Text("Пример предложения (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = outlinedFieldColors(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(4.dp))

            Text("Фото к слову (необязательно)", color = TextSecondary, fontSize = 13.sp)

            if (imageUri.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { imageUri = "" },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = ColorError, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(0.5f))
                ) {
                    Icon(Icons.Default.Image, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (imageUri.isEmpty()) "Выбрать фото" else "Изменить фото",
                        color = Primary, fontSize = 13.sp
                    )
                }
                if (english.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            if (!isImageGenerating) {
                                isImageGenerating = true
                                scope.launch {
                                    val url = ImageApiService.searchPhoto(english.trim())
                                    if (url != null) {
                                        imageUri = url
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "Не удалось найти фото. Проверь API‑ключ или попробуй другое слово.",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                    isImageGenerating = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Secondary.copy(0.5f))
                    ) {
                        if (isImageGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Secondary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            if (isImageGenerating) "Ищу фото..." else "AI фото",
                            color = Secondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            var isSaving by rememberSaveable { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        onSave(
                            Word(
                                id = if (wordId != -1L) wordId else 0,
                                deckId = deckId,
                                english = english.trim(),
                                russian = russian.trim(),
                                transcription = transcription.trim(),
                                example = example.trim(),
                                exampleTranslation = exampleTranslation.trim(),
                                imageUri = imageUri.trim(),
                                cloudId = wordCloudId
                            )
                        )
                    }
                },
                enabled = english.isNotBlank() && russian.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEdit) "Сохранить изменения" else "Добавить слово",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExampleCard(ex: LevelExample, onSelect: (String, String) -> Unit) {
    val levelColor = when (ex.level) {
        "A1", "A2" -> ColorSuccess
        "B1" -> ColorWarning
        else -> ColorError
    }
    Card(
        onClick = { onSelect(ex.english, ex.russian) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(levelColor.copy(0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(ex.level, color = levelColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(ex.english, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                Text(ex.russian, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}