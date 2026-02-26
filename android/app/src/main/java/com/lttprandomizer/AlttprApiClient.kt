package com.lttprandomizer

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object AlttprApiClient {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "LTTPRandomizerGenerator-Android/0.1")
                    .build()
            )
        }
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private const val BASE = "https://alttpr.com"

    data class SeedResult(
        val hash: String,
        val permalink: String,
        val bpsBytes: ByteArray,
        val dictPatches: List<Map<String, List<Int>>>,
        val sizeMb: Int,
    )

    /**
     * Generates a seed and returns [SeedResult].
     * Throws [IOException] or [IllegalStateException] on failure.
     * Call from a coroutine (not the main thread).
     */
    fun generate(settings: RandomizerSettings, onProgress: (String) -> Unit): SeedResult {
        onProgress("Contacting alttpr.com…")

        val body = json.encodeToString(settings).toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$BASE/api/randomizer")
            .post(body)
            .build()

        val apiResponse: SeedApiResponse = http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful)
                throw IOException("API error ${resp.code}: ${resp.message}")
            val raw = resp.body?.string() ?: throw IOException("Empty API response")
            json.decodeFromString(raw)
        }

        onProgress("Downloading base patch…")
        val bpsBytes = http.newCall(Request.Builder().url("$BASE${apiResponse.bpsLocation}").build())
            .execute().use { resp ->
                if (!resp.isSuccessful)
                    throw IOException("Failed to download BPS patch: ${resp.code}")
                resp.body?.bytes() ?: throw IOException("Empty BPS response")
            }

        return SeedResult(
            hash       = apiResponse.hash,
            permalink  = "$BASE/h/${apiResponse.hash}",
            bpsBytes   = bpsBytes,
            dictPatches = apiResponse.patch,
            sizeMb     = apiResponse.size,
        )
    }
}
