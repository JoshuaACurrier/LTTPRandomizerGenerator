using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Text.Json.Nodes;
using LTTPRandomizerGenerator.Models;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Loads and saves user-defined presets to %AppData%\LTTPRandomizerGenerator\presets.json.
    /// Auto-saves last-used settings as the "_last" pseudo-preset.
    /// </summary>
    public static class PresetManager
    {
        private static readonly string DataDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "LTTPRandomizerGenerator");

        private static readonly string PresetsFile = Path.Combine(DataDir, "presets.json");
        private static readonly string LastFile    = Path.Combine(DataDir, "last_settings.json");
        private static readonly string PathsFile   = Path.Combine(DataDir, "paths.json");

        private static readonly JsonSerializerOptions JsonOpts = new() { WriteIndented = true };

        /// <summary>Set to true if any load operation failed to deserialize saved data.</summary>
        public static bool LastLoadHadError { get; private set; }

        // ── Load ──────────────────────────────────────────────────────────────

        /// <summary>Returns user-saved presets (does NOT include built-in presets).</summary>
        public static List<RandomizerPreset> LoadUserPresets()
        {
            if (!File.Exists(PresetsFile)) return new();
            try
            {
                string json = File.ReadAllText(PresetsFile);
                return JsonSerializer.Deserialize<List<RandomizerPreset>>(json) ?? new();
            }
            catch { LastLoadHadError = true; return new(); }
        }

        /// <summary>Returns the last-used settings, or default settings if none saved.</summary>
        public static RandomizerSettings LoadLastSettings()
        {
            if (!File.Exists(LastFile)) return new();
            try
            {
                string json = File.ReadAllText(LastFile);
                return JsonSerializer.Deserialize<RandomizerSettings>(json) ?? new();
            }
            catch { LastLoadHadError = true; return new(); }
        }

        // ── Save ──────────────────────────────────────────────────────────────

        /// <summary>
        /// Saves or updates a user preset by name. Built-in presets cannot be saved over.
        /// Returns an error message or null on success.
        /// </summary>
        public static string? SavePreset(string name, RandomizerSettings settings)
        {
            if (string.IsNullOrWhiteSpace(name))
                return "Preset name cannot be empty.";

            // Guard against overwriting built-in names
            foreach (var builtIn in BuiltInPresets.All)
                if (builtIn.Name.Equals(name, StringComparison.OrdinalIgnoreCase))
                    return $"\"{name}\" is a built-in preset and cannot be overwritten.";

            var presets = LoadUserPresets();
            var existing = presets.Find(p => p.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
            if (existing is not null)
                existing.Settings = settings;
            else
                presets.Add(new RandomizerPreset { Name = name, Settings = settings });

            return WritePresetsFile(presets);
        }

        /// <summary>
        /// Deletes a user preset by name. Returns an error message or null on success.
        /// </summary>
        public static string? DeletePreset(string name)
        {
            foreach (var builtIn in BuiltInPresets.All)
                if (builtIn.Name.Equals(name, StringComparison.OrdinalIgnoreCase))
                    return $"\"{name}\" is a built-in preset and cannot be deleted.";

            var presets = LoadUserPresets();
            int removed = presets.RemoveAll(p => p.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
            if (removed == 0) return $"Preset \"{name}\" not found.";
            return WritePresetsFile(presets);
        }

        /// <summary>Persists last-used settings for auto-restore on next launch.</summary>
        public static void SaveLastSettings(RandomizerSettings settings)
        {
            try
            {
                Directory.CreateDirectory(DataDir);
                File.WriteAllText(LastFile, JsonSerializer.Serialize(settings, JsonOpts));
            }
            catch { /* non-fatal */ }
        }

        /// <summary>Persists ROM path and output folder for auto-restore on next launch.</summary>
        public static void SavePaths(string romPath, string outputFolder)
        {
            try
            {
                Directory.CreateDirectory(DataDir);
                var obj = new { romPath, outputFolder };
                File.WriteAllText(PathsFile, JsonSerializer.Serialize(obj, JsonOpts));
            }
            catch { /* non-fatal */ }
        }

        /// <summary>Returns (romPath, outputFolder) saved from the last session, or empty strings.</summary>
        public static (string RomPath, string OutputFolder) LoadPaths()
        {
            if (!File.Exists(PathsFile)) return (string.Empty, string.Empty);
            try
            {
                using var doc = JsonDocument.Parse(File.ReadAllText(PathsFile));
                var root = doc.RootElement;
                string rom    = root.TryGetProperty("romPath",      out var r) ? r.GetString() ?? "" : "";
                string output = root.TryGetProperty("outputFolder", out var o) ? o.GetString() ?? "" : "";
                return (rom, output);
            }
            catch { LastLoadHadError = true; return (string.Empty, string.Empty); }
        }

        // ── Internal ─────────────────────────────────────────────────────────

        private static string? WritePresetsFile(List<RandomizerPreset> presets)
        {
            try
            {
                Directory.CreateDirectory(DataDir);
                File.WriteAllText(PresetsFile, JsonSerializer.Serialize(presets, JsonOpts));
                return null;
            }
            catch (Exception ex)
            {
                return $"Failed to save presets: {ex.Message}";
            }
        }
    }
}
