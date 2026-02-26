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
        private const uint ExpectedCrc32 = 0x777_AAC_3Fu; // known-good CRC32

        // Some ROM dumps include a 512-byte copier header — strip it if present
        private const int CopierHeaderSize = 512;

        /// <summary>
        /// Returns null if the ROM is valid, or an error message if not.
        /// Also writes back the headerless bytes via <paramref name="romBytes"/>
        /// so the caller can use them directly for patching.
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
                return $"ROM size is {raw.Length:N0} bytes. Expected {ExpectedSizeBytes:N0} bytes (Japanese v1.0 ALttP, headerless).";

            uint crc = Crc32(raw);
            if (crc != ExpectedCrc32)
                return $"ROM CRC32 mismatch (got 0x{crc:X8}, expected 0x{ExpectedCrc32:X8}).\n" +
                       "This randomizer requires the Japanese v1.0 \"Zelda no Densetsu: Kamigami no Triforce\" ROM.";

            romBytes = raw;
            return null;
        }

        private static uint Crc32(byte[] data)
        {
            uint crc = 0xFFFFFFFF;
            foreach (byte b in data)
            {
                crc ^= b;
                for (int i = 0; i < 8; i++)
                    crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xEDB88320 : crc >> 1;
            }
            return ~crc;
        }
    }
}
