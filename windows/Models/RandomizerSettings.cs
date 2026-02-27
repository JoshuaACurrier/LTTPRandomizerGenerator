using System.Text.Json.Serialization;

namespace LTTPRandomizerGenerator.Models
{
    /// <summary>
    /// Maps to the alttpr.com POST /api/randomizer request body.
    /// Field names match the current PHP controller's $request->input() calls.
    /// Note: crystals, item, and enemizer are nested JSON objects.
    /// </summary>
    public class RandomizerSettings
    {
        [JsonPropertyName("glitches")]
        public string Glitches { get; set; } = "none";

        [JsonPropertyName("item_placement")]
        public string ItemPlacement { get; set; } = "basic";

        [JsonPropertyName("dungeon_items")]
        public string DungeonItems { get; set; } = "standard";

        [JsonPropertyName("accessibility")]
        public string Accessibility { get; set; } = "items";

        [JsonPropertyName("goal")]
        public string Goal { get; set; } = "ganon";

        /// <summary>Crystal requirements (nested: crystals.tower / crystals.ganon)</summary>
        [JsonPropertyName("crystals")]
        public CrystalsSettings Crystals { get; set; } = new();

        /// <summary>World state / game mode. Was "world_state" in older API versions.</summary>
        [JsonPropertyName("mode")]
        public string Mode { get; set; } = "open";

        /// <summary>Entrance shuffle. Was "entrance_shuffle" in older API versions.</summary>
        [JsonPropertyName("entrances")]
        public string Entrances { get; set; } = "none";

        [JsonPropertyName("hints")]
        public string Hints { get; set; } = "on";

        [JsonPropertyName("weapons")]
        public string Weapons { get; set; } = "randomized";

        /// <summary>Item pool and functionality (nested: item.pool / item.functionality)</summary>
        [JsonPropertyName("item")]
        public ItemSettings Item { get; set; } = new();

        [JsonPropertyName("spoilers")]
        public string Spoilers { get; set; } = "on";

        /// <summary>
        /// Start with Pegasus Boots. Was "eq": ["PegasusBoots"] in older API versions.
        /// </summary>
        [JsonPropertyName("pseudoboots")]
        public bool Pseudoboots { get; set; } = false;

        /// <summary>Enemizer settings (nested object)</summary>
        [JsonPropertyName("enemizer")]
        public EnemizerSettings Enemizer { get; set; } = new();

        [JsonPropertyName("lang")]
        public string Lang { get; set; } = "en";
    }

    public class CrystalsSettings
    {
        [JsonPropertyName("tower")]
        public string Tower { get; set; } = "7";

        [JsonPropertyName("ganon")]
        public string Ganon { get; set; } = "7";
    }

    public class ItemSettings
    {
        [JsonPropertyName("pool")]
        public string Pool { get; set; } = "normal";

        [JsonPropertyName("functionality")]
        public string Functionality { get; set; } = "normal";
    }

    public class EnemizerSettings
    {
        [JsonPropertyName("boss_shuffle")]
        public string BossShuffle { get; set; } = "none";

        [JsonPropertyName("enemy_shuffle")]
        public string EnemyShuffle { get; set; } = "none";

        [JsonPropertyName("enemy_damage")]
        public string EnemyDamage { get; set; } = "default";

        [JsonPropertyName("enemy_health")]
        public string EnemyHealth { get; set; } = "default";

        [JsonPropertyName("pot_shuffle")]
        public string PotShuffle { get; set; } = "off";
    }

    // ── Display option helpers used by the UI ComboBoxes ──────────────────────

    public record DropdownOption(string Display, string ApiValue)
    {
        public override string ToString() => Display;
    }

    public static class SettingsOptions
    {
        public static readonly DropdownOption[] Glitches =
        [
            new("None",                 "none"),
            new("Minor Glitches",       "minor_glitches"),
            new("Overworld Glitches",   "overworld_glitches"),
            new("Major Glitches",       "major_glitches"),
        ];

        public static readonly DropdownOption[] ItemPlacement =
        [
            new("Basic",    "basic"),
            new("Advanced", "advanced"),
        ];

        public static readonly DropdownOption[] DungeonItems =
        [
            new("Standard (keys stay in dungeons)",         "standard"),
            new("Maps & Compasses",                         "mc"),
            new("Maps, Compasses & Small Keys",             "mcs"),
            new("Keysanity (everything goes anywhere)",     "full"),
        ];

        public static readonly DropdownOption[] Accessibility =
        [
            new("Items (can beat the game)",    "items"),
            new("Locations (100% reachable)",   "locations"),
            new("None (no guarantee)",          "none"),
        ];

        public static readonly DropdownOption[] Goal =
        [
            new("Defeat Ganon",             "ganon"),
            new("Fast Ganon",               "fast_ganon"),
            new("All Dungeons",             "dungeons"),
            new("Master Sword Pedestal",    "pedestal"),
        ];

        public static readonly DropdownOption[] CrystalCount =
        [
            new("0", "0"), new("1", "1"), new("2", "2"), new("3", "3"),
            new("4", "4"), new("5", "5"), new("6", "6"), new("7", "7"),
        ];

        public static readonly DropdownOption[] WorldState =
        [
            new("Open",     "open"),
            new("Standard", "standard"),
            new("Inverted", "inverted"),
            new("Retro",    "retro"),
        ];

        public static readonly DropdownOption[] EntranceShuffle =
        [
            new("None",         "none"),
            new("Simple",       "simple"),
            new("Restricted",   "restricted"),
            new("Full",         "full"),
            new("Crossed",      "crossed"),
            new("Insanity",     "insanity"),
        ];

        public static readonly DropdownOption[] BossShuffle =
        [
            new("None",     "none"),
            new("Simple",   "simple"),
            new("Full",     "full"),
            new("Random",   "random"),
        ];

        public static readonly DropdownOption[] EnemyShuffle =
        [
            new("None",     "none"),
            new("Shuffled", "shuffled"),
            new("Random",   "random"),
        ];

        public static readonly DropdownOption[] EnemyDamage =
        [
            new("Default",  "default"),
            new("Half",     "half"),
            new("Double",   "double"),
            new("Quad",     "quad"),
        ];

        public static readonly DropdownOption[] EnemyHealth =
        [
            new("Default",  "default"),
            new("Easy",     "easy"),
            new("Hard",     "hard"),
            new("Expert",   "expert"),
        ];

        public static readonly DropdownOption[] PotShuffle =
        [
            new("Off", "off"),
            new("On",  "on"),
        ];

        public static readonly DropdownOption[] Hints =
        [
            new("On",  "on"),
            new("Off", "off"),
        ];

        public static readonly DropdownOption[] Weapons =
        [
            new("Randomized",   "randomized"),
            new("Assured",      "assured"),
            new("Vanilla",      "vanilla"),
            new("Swordless",    "swordless"),
        ];

        public static readonly DropdownOption[] ItemPool =
        [
            new("Normal",       "normal"),
            new("Hard",         "hard"),
            new("Expert",       "expert"),
            new("Crowd Control","crowd_control"),
        ];

        public static readonly DropdownOption[] ItemFunctionality =
        [
            new("Normal",   "normal"),
            new("Hard",     "hard"),
            new("Expert",   "expert"),
        ];

        public static readonly DropdownOption[] Spoilers =
        [
            new("On",           "on"),
            new("Off",          "off"),
            new("Exclusions",   "generate"),
        ];

        public static readonly DropdownOption[] PegasusBoots =
        [
            new("Off (normal start)",   "off"),
            new("Start with Boots",     "on"),
        ];
    }
}
