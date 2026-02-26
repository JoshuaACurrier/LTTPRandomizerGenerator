using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading;
using System.Threading.Tasks;
using LTTPRandomizerGenerator.Models;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Communicates with the alttpr.com API to generate a randomizer seed
    /// and retrieve the patch data needed to produce the output ROM.
    /// </summary>
    public static class AlttprApiClient
    {
        private static readonly HttpClient Http = new()
        {
            BaseAddress = new Uri("https://alttpr.com"),
            Timeout = TimeSpan.FromSeconds(60),
        };

        static AlttprApiClient()
        {
            Http.DefaultRequestHeaders.Add("User-Agent", "LTTPRandomizerGenerator/0.1");
        }

        /// <summary>
        /// Posts settings to the API and returns the seed result.
        /// Returns null on failure and sets errorMessage.
        /// </summary>
        public static async Task<SeedResult?> GenerateAsync(
            RandomizerSettings settings,
            IProgress<string>? progress = null,
            CancellationToken ct = default)
        {
            progress?.Report("Contacting alttpr.com...");

            HttpResponseMessage response;
            try
            {
                response = await Http.PostAsJsonAsync("/api/randomizer", settings, ct);
            }
            catch (HttpRequestException ex)
            {
                throw new InvalidOperationException($"Network error: {ex.Message}", ex);
            }
            catch (TaskCanceledException)
            {
                throw new InvalidOperationException("Request timed out. Check your internet connection.");
            }

            if (!response.IsSuccessStatusCode)
            {
                string body = await response.Content.ReadAsStringAsync(ct);
                throw new InvalidOperationException(
                    $"API returned {(int)response.StatusCode}: {response.ReasonPhrase}\n{body}");
            }

            progress?.Report("Parsing seed data...");
            var apiResponse = await response.Content.ReadFromJsonAsync<ApiResponse>(ct);
            if (apiResponse is null)
                throw new InvalidOperationException("API returned empty response.");

            // Download the BPS base patch
            progress?.Report("Downloading base patch...");
            byte[] bpsBytes;
            try
            {
                bpsBytes = await Http.GetByteArrayAsync(apiResponse.BpsLocation, ct);
            }
            catch (Exception ex)
            {
                throw new InvalidOperationException($"Failed to download BPS patch: {ex.Message}", ex);
            }

            return new SeedResult
            {
                Hash = apiResponse.Hash,
                Permalink = $"https://alttpr.com/h/{apiResponse.Hash}",
                BpsPatchBytes = bpsBytes,
                DictionaryPatches = apiResponse.Patch,
                RomSizeMb = apiResponse.Size,
            };
        }

        // ── Internal deserialization types ────────────────────────────────────

        private class ApiResponse
        {
            [JsonPropertyName("hash")]
            public string Hash { get; set; } = string.Empty;

            [JsonPropertyName("patch")]
            public List<Dictionary<string, List<int>>> Patch { get; set; } = new();

            [JsonPropertyName("size")]
            public int Size { get; set; } = 2;

            [JsonPropertyName("bpsLocation")]
            public string BpsLocation { get; set; } = string.Empty;
        }
    }

    public class SeedResult
    {
        public string Hash { get; set; } = string.Empty;
        public string Permalink { get; set; } = string.Empty;
        public byte[] BpsPatchBytes { get; set; } = Array.Empty<byte>();
        public List<Dictionary<string, List<int>>> DictionaryPatches { get; set; } = new();
        public int RomSizeMb { get; set; } = 2;
    }
}
