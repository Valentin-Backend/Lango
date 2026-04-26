package com.lango.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiCardResult(
    val translation: String,
    val transcription: String,
    val examples: List<LevelExample>
)

data class LevelExample(
    val level: String,
    val english: String,
    val russian: String
)

object GroqApiService {
    private const val API_KEY = "YOUR_GROQ_API_KEY"
    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "llama-3.3-70b-versatile"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateCardData(word: String): AiCardResult? = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Generate language learning data for the English word: "$word"
                
                Respond ONLY with a valid JSON object, no markdown, no explanations:
                {
                  "translation": "перевод на русский (несколько значений через запятую)",
                  "transcription": "[фонетическая транскрипция]",
                  "examples": [
                    {
                      "level": "A1",
                      "english": "простое предложение уровня A1",
                      "russian": "перевод предложения"
                    },
                    {
                      "level": "B1",
                      "english": "предложение уровня B1",
                      "russian": "перевод предложения"
                    },
                    {
                      "level": "B2",
                      "english": "сложное предложение уровня B2",
                      "russian": "перевод предложения"
                    }
                  ]
                }
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 500)
                put("temperature", 0.7)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null

            val json = JSONObject(responseBody)
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            val resultJson = JSONObject(content)
            val examplesArray = resultJson.getJSONArray("examples")
            val examples = (0 until examplesArray.length()).map { i ->
                val ex = examplesArray.getJSONObject(i)
                LevelExample(
                    level = ex.getString("level"),
                    english = ex.getString("english"),
                    russian = ex.getString("russian")
                )
            }

            AiCardResult(
                translation = resultJson.getString("translation"),
                transcription = resultJson.getString("transcription"),
                examples = examples
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun validateWord(word: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val prompt = """Is "$word" a real English word or phrase that exists in dictionaries?
Answer ONLY with JSON: {"valid": true} or {"valid": false}"""
            val body = JSONObject().apply {
                put("model", MODEL); put("max_tokens", 50); put("temperature", 0)
                put("messages", JSONArray().apply { put(JSONObject().apply { put("role","user"); put("content", prompt) }) })
            }.toString()
            val request = Request.Builder().url(BASE_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            val response = client.newCall(request).execute()
            val content = JSONObject(response.body?.string() ?: return@withContext false)
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim()
            JSONObject(content.replace("```json","").replace("```","").trim()).getBoolean("valid")
        } catch (e: Exception) { true }
    }

    data class AiDeckResult(
        val title: String, val description: String, val emoji: String,
        val words: List<AiDeckWord>
    )
    data class AiDeckWord(
        val english: String, val russian: String, val transcription: String,
        val example: String, val exampleTranslation: String
    )

    enum class AiDeckMode { WORDS, PHRASES }

    private fun extractJsonObject(text: String): String? {
        val clean = text.replace("```json", "").replace("```", "").trim()
        val start = clean.indexOf('{')
        val end = clean.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return clean.substring(start, end + 1)
    }

    private fun buildDeckPrompt(topic: String, count: Int, avoidEnglish: List<String>, mode: AiDeckMode): String {
        val avoid = if (avoidEnglish.isNotEmpty()) {
            "\nAvoid duplicates. Do NOT use these exact english items:\n- " + avoidEnglish.joinToString("\n- ")
        } else ""

        val rules = if (mode == AiDeckMode.WORDS) {
            """Rules:
- Generate EXACTLY $count items in the "words" array.
- Each "english" must be ONE word (or a short collocation up to 3 words). No full sentences.
- No punctuation in "english" (no '.', '?', '!', ',', ';', ':').
- For EACH item, provide:
  - "russian" translation
  - "transcription" (IPA in square brackets, e.g. "[wɜːd]" or "[ˈtrævəl]")
  - "example" (an English sentence using the word)
  - "exampleTranslation" (Russian translation of the example)
"""
        } else {
            """Rules:
- Generate EXACTLY $count items in the "words" array.
- Each "english" must be a practical phrase or short sentence (3–12 words), NOT a single word.
- Keep items unique and strongly related to the topic.
- You may keep "example" and "exampleTranslation" empty if "english" is already a full phrase.
"""
        }

        return """You are creating a deck for Russian learners of English.
Topic: "$topic"

$rules
- Return ONLY valid JSON (no markdown, no explanations).
$avoid

JSON format:
{
  "title": "Deck title in Russian",
  "description": "Brief description in Russian",
  "emoji": "single relevant emoji",
  "words": [
    {
      "english": "item",
      "russian": "перевод",
      "transcription": "[ipa]",
      "example": "",
      "exampleTranslation": ""
    }
  ]
}"""
    }

    private suspend fun requestDeckJson(topic: String, count: Int, avoidEnglish: List<String>, mode: AiDeckMode): JSONObject? {
        val prompt = buildDeckPrompt(topic, count, avoidEnglish, mode)
        val maxTokens = (900 + count * 60).coerceIn(1500, 8000)

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", maxTokens)
            put("temperature", 0.4)
            put(
                "messages",
                JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }
            )
        }.toString()

        val request = Request.Builder().url(BASE_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType())).build()

        val resp = client.newCall(request).execute()
        val content = JSONObject(resp.body?.string() ?: return null)
            .getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")

        val jsonText = extractJsonObject(content) ?: return null
        return JSONObject(jsonText)
    }

    suspend fun generateDeck(topic: String, requestedCount: Int, mode: AiDeckMode): AiDeckResult? = withContext(Dispatchers.IO) {
        try {
            val count = requestedCount.coerceIn(10, 50)
            var title: String? = null
            var description: String? = null
            var emoji: String? = null

            val collected = mutableListOf<AiDeckWord>()
            val seen = linkedSetOf<String>()

            var attempts = 0
            while (collected.size < count && attempts < 5) {
                attempts++
                val remaining = count - collected.size
                val chunk = minOf(remaining, 25)
                val avoid = seen.take(40).toList()

                val json = requestDeckJson(topic, chunk, avoid, mode) ?: continue
                if (title == null) title = json.optString("title", "Новая колода")
                if (description == null) description = json.optString("description", "")
                if (emoji == null) emoji = json.optString("emoji", "📚")

                val arr = json.optJSONArray("words") ?: continue
                for (i in 0 until arr.length()) {
                    val w = arr.optJSONObject(i) ?: continue
                    val en = w.optString("english", "").trim()
                    val ru = w.optString("russian", "").trim()
                    if (en.isBlank() || ru.isBlank()) continue
                    val example = w.optString("example", "").trim()
                    val exampleTr = w.optString("exampleTranslation", "").trim()
                    val transcription = w.optString("transcription", "").trim()

                    if (mode == AiDeckMode.PHRASES) {
                        if (!en.contains(" ")) continue
                    } else {
                        val parts = en.split(Regex("\\s+")).filter { it.isNotBlank() }
                        if (parts.isEmpty() || parts.size > 3) continue
                        if (en.any { it in listOf('.', '?', '!', ',', ';', ':') }) continue
                        if (transcription.isBlank()) continue
                        if (example.isBlank() || exampleTr.isBlank()) continue
                    }
                    val key = en.lowercase()
                    if (seen.contains(key)) continue
                    seen.add(key)

                    collected.add(
                        AiDeckWord(
                            english = en,
                            russian = ru,
                            transcription = transcription,
                            example = example,
                            exampleTranslation = exampleTr
                        )
                    )
                    if (collected.size >= count) break
                }
            }

            if (collected.size < count) return@withContext null

            AiDeckResult(
                title = title ?: "Новая колода",
                description = description ?: "",
                emoji = emoji ?: "📚",
                words = collected.take(count)
            )
        } catch (e: Exception) { null }
    }
}
