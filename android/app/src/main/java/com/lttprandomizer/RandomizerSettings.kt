package com.lttprandomizer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RandomizerSettings(
    @SerialName("glitches")           val glitches: String           = "none",
    @SerialName("item_placement")     val itemPlacement: String      = "advanced",
    @SerialName("dungeon_items")      val dungeonItems: String       = "standard",
    @SerialName("accessibility")      val accessibility: String      = "items",
    @SerialName("goal")               val goal: String               = "ganon",
    @SerialName("tower_open")         val towerOpen: String          = "7",
    @SerialName("ganon_open")         val ganonOpen: String          = "7",
    @SerialName("world_state")        val worldState: String         = "open",
    @SerialName("entrance_shuffle")   val entranceShuffle: String    = "none",
    @SerialName("boss_shuffle")       val bossShuffle: String        = "none",
    @SerialName("enemy_shuffle")      val enemyShuffle: String       = "none",
    @SerialName("hints")              val hints: String              = "on",
    @SerialName("weapons")            val weapons: String            = "randomized",
    @SerialName("item_pool")          val itemPool: String           = "normal",
    @SerialName("item_functionality") val itemFunctionality: String  = "normal",
    @SerialName("spoilers")           val spoilers: String           = "on",
    @SerialName("eq")                 val eq: List<String>           = emptyList(),
    @SerialName("lang")               val lang: String               = "en",
)

// ── API response model ────────────────────────────────────────────────────────

@Serializable
data class SeedApiResponse(
    @SerialName("hash")        val hash: String,
    @SerialName("patch")       val patch: List<Map<String, List<Int>>> = emptyList(),
    @SerialName("size")        val size: Int = 2,
    @SerialName("bpsLocation") val bpsLocation: String = "",
)

// ── Preset ────────────────────────────────────────────────────────────────────

@Serializable
data class RandomizerPreset(
    val name: String,
    val settings: RandomizerSettings,
)

// ── Built-in presets ──────────────────────────────────────────────────────────

object BuiltInPresets {
    val all = listOf(
        RandomizerPreset("Quick Run", RandomizerSettings(
            goal = "fast_ganon", itemPlacement = "basic",
        )),
        RandomizerPreset("Casual Boots", RandomizerSettings(
            itemPlacement = "basic", eq = listOf("PegasusBoots"),
        )),
        RandomizerPreset("Keysanity", RandomizerSettings(
            itemPlacement = "advanced", dungeonItems = "full",
        )),
        RandomizerPreset("All Mix", RandomizerSettings(
            itemPlacement = "advanced", dungeonItems = "full",
            entranceShuffle = "crossed", bossShuffle = "full", enemyShuffle = "shuffled",
        )),
        RandomizerPreset("Beginner", RandomizerSettings(
            itemPlacement = "basic", accessibility = "locations",
        )),
        RandomizerPreset("Swordless", RandomizerSettings(
            weapons = "swordless", itemPlacement = "advanced",
        )),
    )
}

// ── Dropdown option ────────────────────────────────────────────────────────────

data class DropdownOption(val display: String, val apiValue: String) {
    override fun toString() = display
}

object SettingsOptions {
    val glitches = listOf(
        DropdownOption("None",                "none"),
        DropdownOption("Minor Glitches",      "minor_glitches"),
        DropdownOption("Overworld Glitches",  "overworld_glitches"),
        DropdownOption("Major Glitches",      "major_glitches"),
    )
    val itemPlacement = listOf(
        DropdownOption("Basic",    "basic"),
        DropdownOption("Advanced", "advanced"),
    )
    val dungeonItems = listOf(
        DropdownOption("Standard",                      "standard"),
        DropdownOption("Maps & Compasses",              "mc"),
        DropdownOption("Maps, Compasses & Small Keys",  "mcs"),
        DropdownOption("Keysanity (full)",              "full"),
    )
    val accessibility = listOf(
        DropdownOption("Items",     "items"),
        DropdownOption("Locations", "locations"),
        DropdownOption("None",      "none"),
    )
    val goal = listOf(
        DropdownOption("Defeat Ganon",          "ganon"),
        DropdownOption("Fast Ganon",            "fast_ganon"),
        DropdownOption("All Dungeons",          "dungeons"),
        DropdownOption("Pedestal",              "pedestal"),
    )
    val crystalCount = (0..7).map { DropdownOption(it.toString(), it.toString()) }
    val worldState = listOf(
        DropdownOption("Open",     "open"),
        DropdownOption("Standard", "standard"),
        DropdownOption("Inverted", "inverted"),
        DropdownOption("Retro",    "retro"),
    )
    val entranceShuffle = listOf(
        DropdownOption("None",       "none"),
        DropdownOption("Simple",     "simple"),
        DropdownOption("Restricted", "restricted"),
        DropdownOption("Full",       "full"),
        DropdownOption("Crossed",    "crossed"),
        DropdownOption("Insanity",   "insanity"),
    )
    val bossShuffle = listOf(
        DropdownOption("None",   "none"),
        DropdownOption("Simple", "simple"),
        DropdownOption("Full",   "full"),
        DropdownOption("Random", "random"),
    )
    val enemyShuffle = listOf(
        DropdownOption("None",     "none"),
        DropdownOption("Shuffled", "shuffled"),
        DropdownOption("Random",   "random"),
    )
    val hints = listOf(DropdownOption("On", "on"), DropdownOption("Off", "off"))
    val weapons = listOf(
        DropdownOption("Randomized", "randomized"),
        DropdownOption("Assured",    "assured"),
        DropdownOption("Vanilla",    "vanilla"),
        DropdownOption("Swordless",  "swordless"),
    )
    val itemPool = listOf(
        DropdownOption("Normal",        "normal"),
        DropdownOption("Hard",          "hard"),
        DropdownOption("Expert",        "expert"),
        DropdownOption("Crowd Control", "crowd_control"),
    )
    val itemFunctionality = listOf(
        DropdownOption("Normal", "normal"),
        DropdownOption("Hard",   "hard"),
        DropdownOption("Expert", "expert"),
    )
    val spoilers = listOf(
        DropdownOption("On",         "on"),
        DropdownOption("Off",        "off"),
        DropdownOption("Exclusions", "generate"),
    )
    val pegasusBoots = listOf(
        DropdownOption("Off (normal start)", "off"),
        DropdownOption("Start with Boots",   "on"),
    )
}
