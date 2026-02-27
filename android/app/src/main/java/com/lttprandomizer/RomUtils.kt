package com.lttprandomizer

/**
 * Shared ROM utilities used by both BpsPatcher and CosmeticPatcher.
 */
object RomUtils {
    /**
     * Recalculates and writes the SNES HiROM checksum at 0x7FDC–0x7FDF.
     * Modifies [rom] in place.
     */
    fun writeChecksum(rom: ByteArray) {
        rom[0x7FDC] = 0; rom[0x7FDD] = 0; rom[0x7FDE] = 0; rom[0x7FDF] = 0

        var sum = 0L
        for (b in rom) sum += (b.toInt() and 0xFF)
        val checksum   = (sum and 0xFFFFL).toInt()
        val complement = checksum xor 0xFFFF

        rom[0x7FDC] = (complement and 0xFF).toByte()
        rom[0x7FDD] = (complement shr 8).toByte()
        rom[0x7FDE] = (checksum and 0xFF).toByte()
        rom[0x7FDF] = (checksum shr 8).toByte()
    }
}
