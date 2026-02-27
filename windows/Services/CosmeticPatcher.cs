using LTTPRandomizerGenerator.Models;

namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Applies cosmetic ROM patches to the expansion area (0x18xxxx) after BPS patching.
    /// Must be called AFTER BpsPatcher.Apply() so the ROM has been expanded to 2 MB.
    /// </summary>
    public static class CosmeticPatcher
    {
        // ROM addresses in the SNES expansion area (valid after BPS expansion to 2 MB)
        private const int AddrHeartBeep = 0x180033;
        private const int AddrHeartColor = 0x187020;
        private const int AddrMenuSpeed  = 0x180048;
        private const int AddrQuickSwap  = 0x18004B;

        /// <summary>
        /// Writes cosmetic bytes into the ROM and recalculates the SNES checksum.
        /// Returns the same array (modified in place) for chaining.
        /// </summary>
        public static byte[] Apply(byte[] rom, CustomizationSettings s)
        {
            rom[AddrHeartBeep] = s.HeartBeepSpeed switch
            {
                "off"     => 0x00,
                "double"  => 0x10,
                "half"    => 0x40,
                "quarter" => 0x80,
                _         => 0x20, // normal
            };

            rom[AddrHeartColor] = s.HeartColor switch
            {
                "blue"   => 0x01,
                "green"  => 0x02,
                "yellow" => 0x03,
                _        => 0x00, // red
            };

            rom[AddrMenuSpeed] = s.MenuSpeed switch
            {
                "half"    => 0x04,
                "double"  => 0x10,
                "triple"  => 0x18,
                "quad"    => 0x20,
                "instant" => 0xE8,
                _         => 0x08, // normal
            };

            rom[AddrQuickSwap] = s.QuickSwap == "on" ? (byte)0x01 : (byte)0x00;

            WriteChecksum(rom);
            return rom;
        }

        /// <summary>
        /// Recalculates and writes the SNES HiROM checksum at 0x7FDC–0x7FDF.
        /// Duplicated from BpsPatcher to avoid coupling to its private implementation.
        /// </summary>
        private static void WriteChecksum(byte[] rom)
        {
            rom[0x7FDC] = rom[0x7FDD] = rom[0x7FDE] = rom[0x7FDF] = 0;

            uint sum = 0;
            foreach (byte b in rom) sum += b;
            ushort checksum   = (ushort)(sum & 0xFFFF);
            ushort complement = (ushort)(checksum ^ 0xFFFF);

            rom[0x7FDC] = (byte)(complement & 0xFF);
            rom[0x7FDD] = (byte)(complement >> 8);
            rom[0x7FDE] = (byte)(checksum & 0xFF);
            rom[0x7FDF] = (byte)(checksum >> 8);
        }
    }
}
