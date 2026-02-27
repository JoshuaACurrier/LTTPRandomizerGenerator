namespace LTTPRandomizerGenerator.Services
{
    /// <summary>
    /// Shared ROM utilities used by both BpsPatcher and CosmeticPatcher.
    /// </summary>
    public static class RomUtils
    {
        /// <summary>
        /// Recalculates and writes the SNES HiROM checksum at 0x7FDC–0x7FDF.
        /// </summary>
        public static void WriteChecksum(byte[] rom)
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
