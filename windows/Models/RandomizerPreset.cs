using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace LTTPRandomizerGenerator.Models
{
    public class RandomizerPreset
    {
        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("settings")]
        public RandomizerSettings Settings { get; set; } = new();

        /// <summary>
        /// True for built-in presets that cannot be deleted by the user.
        /// Not persisted to JSON — hardcoded presets are never saved to file.
        /// </summary>
        [JsonIgnore]
        public bool IsBuiltIn { get; set; }

        public override string ToString() => Name;
    }

    public static class BuiltInPresets
    {
        public static IReadOnlyList<RandomizerPreset> All { get; } = new List<RandomizerPreset>
        {
            new()
            {
                Name = "Quick Run",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "fast_ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "basic",
                    DungeonItems = "standard",
                    Accessibility = "items",
                    WorldState = "open",
                    EntranceShuffle = "none",
                    BossShuffle = "none",
                    EnemyShuffle = "none",
                    Hints = "on",
                    Weapons = "randomized",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                }
            },
            new()
            {
                Name = "Casual Boots",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "basic",
                    DungeonItems = "standard",
                    Accessibility = "items",
                    WorldState = "open",
                    EntranceShuffle = "none",
                    BossShuffle = "none",
                    EnemyShuffle = "none",
                    Hints = "on",
                    Weapons = "randomized",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                    StartingEquipment = ["PegasusBoots"],
                }
            },
            new()
            {
                Name = "Keysanity",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "advanced",
                    DungeonItems = "full",
                    Accessibility = "items",
                    WorldState = "open",
                    EntranceShuffle = "none",
                    BossShuffle = "none",
                    EnemyShuffle = "none",
                    Hints = "on",
                    Weapons = "randomized",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                }
            },
            new()
            {
                Name = "All Mix",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "advanced",
                    DungeonItems = "full",
                    Accessibility = "items",
                    WorldState = "open",
                    EntranceShuffle = "crossed",
                    BossShuffle = "full",
                    EnemyShuffle = "shuffled",
                    Hints = "on",
                    Weapons = "randomized",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                }
            },
            new()
            {
                Name = "Beginner",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "basic",
                    DungeonItems = "standard",
                    Accessibility = "locations",
                    WorldState = "open",
                    EntranceShuffle = "none",
                    BossShuffle = "none",
                    EnemyShuffle = "none",
                    Hints = "on",
                    Weapons = "randomized",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                }
            },
            new()
            {
                Name = "Swordless",
                IsBuiltIn = true,
                Settings = new()
                {
                    Goal = "ganon",
                    TowerOpen = "7",
                    GanonOpen = "7",
                    ItemPlacement = "advanced",
                    DungeonItems = "standard",
                    Accessibility = "items",
                    WorldState = "open",
                    EntranceShuffle = "none",
                    BossShuffle = "none",
                    EnemyShuffle = "none",
                    Hints = "on",
                    Weapons = "swordless",
                    ItemPool = "normal",
                    ItemFunctionality = "normal",
                    Spoilers = "on",
                }
            },
        };
    }
}
