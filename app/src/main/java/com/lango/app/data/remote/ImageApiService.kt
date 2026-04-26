package com.lango.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ImageApiService {

    private const val API_KEY = "YOUR_PEXELS_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun searchPhoto(query: String): String? = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank()) return@withContext null
        try {
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("api.pexels.com")
                .addPathSegment("v1")
                .addPathSegment("search")
                .addQueryParameter("query", query)
                .addQueryParameter("per_page", "1")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", API_KEY)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = JSONObject(body)
            val photos = json.optJSONArray("photos") ?: return@withContext null
            if (photos.length() == 0) return@withContext null
            val first = photos.getJSONObject(0)
            val src = first.optJSONObject("src") ?: return@withContext null
            src.optString("large", null) ?: src.optString("medium", null)
        } catch (_: Exception) {
            null
        }
    }
}
