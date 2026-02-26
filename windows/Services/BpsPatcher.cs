using System;
using System.Collections.Generic;
using System.IO;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Applies the two-stage alttpr.com patch to a base ROM.
    ///
    /// Stage 1: BPS (Beat Patch System) — expands/modifies the base ROM structure.
    /// Stage 2: Dictionary patches — seed-specific item/enemy placement bytes.
    /// Stage 3: SNES checksum recalculation.
    /// </summary>
    public static class BpsPatcher
    {
        /// <summary>
        /// Returns the patched ROM bytes, or throws on error.
        /// </summary>
        public static byte[] Apply(
            byte[] sourceRom,
            byte[] bpsPatch,
            List<Dictionary<string, List<int>>> dictPatches,
            int targetSizeMb)
        {
            byte[] rom = ApplyBps(sourceRom, bpsPatch);
            rom = ExpandIfNeeded(rom, targetSizeMb);
            ApplyDictPatches(rom, dictPatches);
            WriteChecksum(rom);
            return rom;
        }

        // ── BPS implementation ────────────────────────────────────────────────

        private static byte[] ApplyBps(byte[] source, byte[] patch)
        {
            if (patch.Length < 4 ||
                patch[0] != 'B' || patch[1] != 'P' || patch[2] != 'S' || patch[3] != '1')
                throw new InvalidDataException("Invalid BPS patch: missing \"BPS1\" magic.");

            int pos = 4;

            ulong ReadVli()
            {
                ulong value = 0, shift = 1;
                while (true)
                {
                    if (pos >= patch.Length)
                        throw new InvalidDataException("BPS patch truncated during VLI read.");
                    byte b = patch[pos++];
                    value += (ulong)(b & 0x7F) * shift;
                    if ((b & 0x80) != 0) break;
                    shift <<= 7;
                    value += shift;
                }
                return value;
            }

            ulong sourceSize = ReadVli();
            ulong targetSize = ReadVli();
            ulong metaLen   = ReadVli();
            pos += (int)metaLen; // skip metadata string

            byte[] output = new byte[targetSize];
            int srcPos  = 0;
            int outPos  = 0;
            int cpySrc  = 0; // SOURCE_COPY relative cursor
            int cpyOut  = 0; // TARGET_COPY relative cursor

            int footerStart = patch.Length - 12;

            while (pos < footerStart)
            {
                ulong action = ReadVli();
                int length = (int)(action >> 2) + 1;
                int type   = (int)(action & 3);

                switch (type)
                {
                    case 0: // SOURCE_READ — copy from source at current output position
                        if (outPos + length > output.Length)
                            throw new InvalidDataException("BPS SOURCE_READ overflows target.");
                        Array.Copy(source, outPos, output, outPos, Math.Min(length, source.Length - outPos));
                        outPos += length;
                        break;

                    case 1: // TARGET_READ — literal bytes embedded in patch
                        for (int i = 0; i < length; i++)
                        {
                            if (pos >= footerStart)
                                throw new InvalidDataException("BPS TARGET_READ truncated.");
                            output[outPos++] = patch[pos++];
                        }
                        break;

                    case 2: // SOURCE_COPY — relative seek into source
                    {
                        ulong d = ReadVli();
                        int delta = (int)(d >> 1);
                        cpySrc += (d & 1) != 0 ? -delta : delta;
                        for (int i = 0; i < length; i++)
                            output[outPos++] = source[cpySrc++];
                        break;
                    }

                    case 3: // TARGET_COPY — relative seek within output (already written)
                    {
                        ulong d = ReadVli();
                        int delta = (int)(d >> 1);
                        cpyOut += (d & 1) != 0 ? -delta : delta;
                        for (int i = 0; i < length; i++)
                            output[outPos++] = output[cpyOut++];
                        break;
                    }
                }
            }

            // Verify patch CRC32 (last 4 bytes of the patch itself, excluding those 4 bytes)
            uint patchCrc = BitConverter.ToUInt32(patch, patch.Length - 4);
            uint computed = Crc32(patch, 0, patch.Length - 4);
            if (computed != patchCrc)
                throw new InvalidDataException(
                    $"BPS patch CRC32 mismatch (got 0x{computed:X8}, expected 0x{patchCrc:X8}). Corrupt download?");

            return output;
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        private static byte[] ExpandIfNeeded(byte[] rom, int targetSizeMb)
        {
            int targetBytes = targetSizeMb * 1024 * 1024;
            if (rom.Length >= targetBytes) return rom;
            byte[] expanded = new byte[targetBytes];
            Array.Copy(rom, expanded, rom.Length);
            return expanded;
        }

        private static void ApplyDictPatches(byte[] rom, List<Dictionary<string, List<int>>> patches)
        {
            foreach (var entry in patches)
            {
                foreach (var (offsetStr, values) in entry)
                {
                    int offset = int.Parse(offsetStr);
                    for (int i = 0; i < values.Count; i++)
                        rom[offset + i] = (byte)values[i];
                }
            }
        }

        /// <summary>
        /// Recalculates and writes the SNES HiROM checksum at 0x7FDC–0x7FDF.
        /// </summary>
        private static void WriteChecksum(byte[] rom)
        {
            // Zero out existing checksum fields before summing
            rom[0x7FDC] = rom[0x7FDD] = rom[0x7FDE] = rom[0x7FDF] = 0;

            uint sum = 0;
            foreach (byte b in rom) sum += b;
            ushort checksum  = (ushort)(sum & 0xFFFF);
            ushort complement = (ushort)(checksum ^ 0xFFFF);

            rom[0x7FDC] = (byte)(complement & 0xFF);
            rom[0x7FDD] = (byte)(complement >> 8);
            rom[0x7FDE] = (byte)(checksum & 0xFF);
            rom[0x7FDF] = (byte)(checksum >> 8);
        }

        private static uint Crc32(byte[] data, int offset, int length)
        {
            uint crc = 0xFFFFFFFF;
            for (int i = offset; i < offset + length; i++)
            {
                crc ^= data[i];
                for (int j = 0; j < 8; j++)
                    crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xEDB88320 : crc >> 1;
            }
            return ~crc;
        }
    }
}
