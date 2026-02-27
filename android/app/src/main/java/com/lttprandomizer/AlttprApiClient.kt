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
    internal val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    internal val http = OkHttpClient.Builder()
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

    @kotlinx.serialization.Serializable
    private data class HashInfo(
        @kotlinx.serialization.SerialName("bpsLocation") val bpsLocation: String = "",
        @kotlinx.serialization.SerialName("md5")         val md5: String = "",
    )

    /**
     * Generates a seed and returns [SeedResult].
     * Throws [IOException] or [IllegalStateException] on failure.
     * Call from a coroutine (not the main thread).
     *
     * Two-step flow (mirrors pyz3r):
     *   1. POST /api/randomizer  → seed hash + dict patches
     *   2. GET  /api/h/{hash}    → bpsLocation for the base BPS patch
     *   3. GET  bpsLocation      → download BPS (English translation + engine)
     */
    fun generate(settings: RandomizerSettings, onProgress: (String) -> Unit): SeedResult {
        onProgress("Contacting alttpr.com…")

        val body = json.encodeToString(settings).toRequestBody(JSON_MEDIA)
        val apiResponse: SeedApiResponse = http.newCall(
            Request.Builder().url("$BASE/api/randomizer").post(body).build()
        ).execute().use { resp ->
            if (!resp.isSuccessful)
                throw IOException("API error ${resp.code}: ${resp.message}")
            val raw = resp.body?.string() ?: throw IOException("Empty API response")
            json.decodeFromString(raw)
        }

        onProgress("Fetching patch metadata…")
        val hashInfo: HashInfo = http.newCall(
            Request.Builder().url("$BASE/api/h/${apiResponse.hash}").build()
        ).execute().use { resp ->
            if (!resp.isSuccessful)
                throw IOException("Failed to fetch patch metadata: ${resp.code}")
            val raw = resp.body?.string() ?: throw IOException("Empty metadata response")
            json.decodeFromString(raw)
        }

        onProgress("Downloading base patch…")
        val bpsBytes = if (hashInfo.bpsLocation.isNotBlank()) {
            val bpsUrl = if (hashInfo.bpsLocation.startsWith("http")) hashInfo.bpsLocation
                         else "$BASE${hashInfo.bpsLocation}"
            http.newCall(Request.Builder().url(bpsUrl).build())
                .execute().use { resp ->
                    if (!resp.isSuccessful)
                        throw IOException("Failed to download BPS patch: ${resp.code}")
                    resp.body?.bytes() ?: throw IOException("Empty BPS response")
                }
        } else ByteArray(0)

        return SeedResult(
            hash        = apiResponse.hash,
            permalink   = "$BASE/h/${apiResponse.hash}",
            bpsBytes    = bpsBytes,
            dictPatches = apiResponse.patch,
            sizeMb      = apiResponse.size,
        )
    }
}
