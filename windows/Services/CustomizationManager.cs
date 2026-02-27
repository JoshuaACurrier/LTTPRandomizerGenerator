using System;
using System.IO;
using System.Text.Json;
using LTTPRandomizerGenerator.Models;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Persists cosmetic customization settings to
    /// %AppData%\LTTPRandomizerGenerator\customization.json.
    /// </summary>
    public static class CustomizationManager
    {
        private static readonly string DataDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "LTTPRandomizerGenerator");

        private static readonly string CustomizationFile = Path.Combine(DataDir, "customization.json");

        private static readonly JsonSerializerOptions JsonOpts = new() { WriteIndented = true };

        public static CustomizationSettings Load()
        {
            if (!File.Exists(CustomizationFile)) return new();
            try
            {
                string json = File.ReadAllText(CustomizationFile);
                return JsonSerializer.Deserialize<CustomizationSettings>(json) ?? new();
            }
            catch { return new(); }
        }

        public static void Save(CustomizationSettings s)
        {
            try
            {
                Directory.CreateDirectory(DataDir);
                File.WriteAllText(CustomizationFile, JsonSerializer.Serialize(s, JsonOpts));
            }
            catch { /* non-fatal */ }
        }
    }
}
