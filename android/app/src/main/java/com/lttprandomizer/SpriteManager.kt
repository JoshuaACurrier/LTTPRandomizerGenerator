package com.lttprandomizer

import android.content.Context
import okhttp3.Request
import java.io.File

/**
 * Singleton responsible for sprite list fetching, ZSPR caching, and random picking.
 * Uses [AlttprApiClient.http] and [AlttprApiClient.json] to avoid creating duplicate clients.
 */
object SpriteManager {
    const val RANDOM_ALL_SENTINEL       = "__random_all__"
    const val RANDOM_FAVORITES_SENTINEL = "__random_favorites__"
    private const val SPRITES_URL       = "https://alttpr.com/sprites"

    /**
     * Fetches the sprite list from cache or network.
     * Must be called from a background thread.
     */
    fun fetchSpriteList(context: Context, forceRefresh: Boolean = false): List<SpriteEntry> {
        val cacheFile = File(context.cacheDir, "sprites_list.json")

        if (!forceRefresh && cacheFile.exists()) {
            try {
                val cached = cacheFile.readText()
                return AlttprApiClient.json.decodeFromString(cached)
            } catch (_: Exception) { /* fall through to network */ }
        }

        val body = AlttprApiClient.http.newCall(
            Request.Builder().url(SPRITES_URL).build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("Failed to fetch sprites: ${resp.code}")
            resp.body?.string() ?: throw java.io.IOException("Empty sprites response")
        }

        // Cache to disk
        cacheFile.writeText(body)

        return AlttprApiClient.json.decodeFromString(body)
    }

    /**
     * Downloads a ZSPR file to the local cache and returns the path.
     * If already cached, returns immediately. Uses atomic write via .tmp rename.
     */
    fun downloadSprite(context: Context, entry: SpriteEntry): String {
        val cacheDir = File(context.cacheDir, "zspr_cache")
        cacheDir.mkdirs()
        val safeName = entry.name.replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".zspr"
        val target = File(cacheDir, safeName)
        if (target.exists()) return target.absolutePath

        val tmp = File(cacheDir, "$safeName.tmp")
        AlttprApiClient.http.newCall(
            Request.Builder().url(entry.file).build()
        ).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("Failed to download sprite: ${resp.code}")
            val bytes = resp.body?.bytes() ?: throw java.io.IOException("Empty sprite download")
            tmp.writeBytes(bytes)
        }
        tmp.renameTo(target)
        return target.absolutePath
    }

    /**
     * Picks a random sprite from the list, optionally filtering to favorites.
     * Returns null if the filtered list is empty.
     */
    fun pickRandom(sprites: List<SpriteEntry>, favorites: Set<String>, favoritesOnly: Boolean): SpriteEntry? {
        val pool = if (favoritesOnly) {
            sprites.filter { it.name in favorites }
        } else {
            sprites
        }
        if (pool.isEmpty()) return null
        return pool.random()
    }

    /**
     * Resolves a sprite path sentinel or direct path to an applied ROM modification.
     * If spritePath is a sentinel, fetches the list, picks random, downloads, and applies.
     * If spritePath is a direct file path, applies it directly.
     * Returns null on success, or an error string.
     */
    fun resolveAndApply(context: Context, spritePath: String, rom: ByteArray): String? {
        if (spritePath.isEmpty()) return null // default Link, nothing to do

        return try {
            val actualPath = when (spritePath) {
                RANDOM_ALL_SENTINEL, RANDOM_FAVORITES_SENTINEL -> {
                    val favoritesOnly = spritePath == RANDOM_FAVORITES_SENTINEL
                    val sprites = fetchSpriteList(context)
                    val favorites = PresetManager.loadFavorites(context)
                    val picked = pickRandom(sprites, favorites, favoritesOnly)
                        ?: return if (favoritesOnly) "No favorite sprites selected. Star some sprites in the browser first."
                           else "Sprite list is empty."
                    downloadSprite(context, picked)
                }
                else -> spritePath
            }
            SpriteApplier.apply(actualPath, rom)
        } catch (e: Exception) {
            "Sprite error: ${e.message}"
        }
    }
}
