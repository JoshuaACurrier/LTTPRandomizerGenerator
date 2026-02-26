package com.lttprandomizer

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PresetManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private const val PREFS_NAME = "lttp_randomizer_prefs"
    private const val KEY_PRESETS = "user_presets"
    private const val KEY_LAST_SETTINGS = "last_settings"

    // ── Load ─────────────────────────────────────────────────────────────────

    fun loadUserPresets(context: Context): List<RandomizerPreset> {
        val raw = prefs(context).getString(KEY_PRESETS, null) ?: return emptyList()
        return try { json.decodeFromString(raw) } catch (_: Exception) { emptyList() }
    }

    fun loadLastSettings(context: Context): RandomizerSettings {
        val raw = prefs(context).getString(KEY_LAST_SETTINGS, null) ?: return RandomizerSettings()
        return try { json.decodeFromString(raw) } catch (_: Exception) { RandomizerSettings() }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    /** Returns an error message or null on success. */
    fun savePreset(context: Context, name: String, settings: RandomizerSettings): String? {
        if (name.isBlank()) return "Preset name cannot be empty."
        if (BuiltInPresets.all.any { it.name.equals(name, ignoreCase = true) })
            return "\"$name\" is a built-in preset and cannot be overwritten."

        val presets = loadUserPresets(context).toMutableList()
        val existing = presets.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (existing >= 0) presets[existing] = RandomizerPreset(name, settings)
        else presets.add(RandomizerPreset(name, settings))

        return writePresets(context, presets)
    }

    /** Returns an error message or null on success. */
    fun deletePreset(context: Context, name: String): String? {
        if (BuiltInPresets.all.any { it.name.equals(name, ignoreCase = true) })
            return "\"$name\" is a built-in preset and cannot be deleted."

        val presets = loadUserPresets(context).toMutableList()
        val removed = presets.removeAll { it.name.equals(name, ignoreCase = true) }
        if (!removed) return "Preset \"$name\" not found."
        return writePresets(context, presets)
    }

    fun saveLastSettings(context: Context, settings: RandomizerSettings) {
        prefs(context).edit().putString(KEY_LAST_SETTINGS, json.encodeToString(settings)).apply()
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun writePresets(context: Context, presets: List<RandomizerPreset>): String? {
        return try {
            prefs(context).edit().putString(KEY_PRESETS, json.encodeToString(presets)).apply()
            null
        } catch (e: Exception) { "Failed to save presets: ${e.message}" }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
