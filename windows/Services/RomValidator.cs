using System.IO;
using System;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Validates that a file is the correct base ROM before patching.
    /// The alttpr.com randomizer requires the Japanese v1.0 ALttP ROM.
    /// </summary>
    public static class RomValidator
    {
        // Japanese v1.0 ALttP — headerless (no 512-byte SMC/FIG header)
        private const int ExpectedSizeBytes = 1_048_576; // 1 MB

        // Some ROM dumps include a 512-byte copier header — strip it if present
        private const int CopierHeaderSize = 512;

        /// <summary>
        /// Returns null if the ROM passes size validation, or an error message if not.
        /// Strips a 512-byte copier header if present and writes the headerless bytes
        /// back via <paramref name="romBytes"/> for use during patching.
        /// Correctness of the ROM content is verified implicitly by the BPS patch
        /// source CRC32 check during patching.
        /// </summary>
        public static string? Validate(string path, out byte[] romBytes)
        {
            romBytes = Array.Empty<byte>();

            if (!File.Exists(path))
                return "ROM file not found.";

            byte[] raw;
            try { raw = File.ReadAllBytes(path); }
            catch (Exception ex) { return $"Cannot read ROM: {ex.Message}"; }

            // Strip 512-byte copier header if present
            if (raw.Length == ExpectedSizeBytes + CopierHeaderSize)
            {
                byte[] stripped = new byte[ExpectedSizeBytes];
                Array.Copy(raw, CopierHeaderSize, stripped, 0, ExpectedSizeBytes);
                raw = stripped;
            }

            if (raw.Length != ExpectedSizeBytes)
                return $"ROM size is {raw.Length:N0} bytes. Expected {ExpectedSizeBytes:N0} bytes.\n" +
                       "This randomizer requires the Japanese v1.0 ALttP ROM (headerless or with 512-byte copier header).";

            romBytes = raw;
            return null;
        }
    }
}
