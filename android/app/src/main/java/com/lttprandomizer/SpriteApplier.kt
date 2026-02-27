package com.lttprandomizer

import java.io.File

/**
 * Validates and applies .zspr / .spr sprite files to a ROM ByteArray.
 * ROM write addresses sourced from pyz3r reference implementation.
 */
object SpriteApplier {
    // ROM addresses for sprite data
    private const val ROM_GFX_OFFSET     = 0x80000   // Sprite pixel data
    private const val ROM_PALETTE_OFFSET = 0xDD308   // Palette data (120 bytes)
    private const val ROM_GLOVES_OFFSET  = 0xDEDF5   // Gloves palette (4 bytes)
    private const val ROM_GFX_MAX_LENGTH = 0x7000    // 28,672 bytes max

    // ZSPR header field byte offsets
    private const val ZSPR_GFX_OFFSET_POS     = 9
    private const val ZSPR_GFX_LENGTH_POS     = 13
    private const val ZSPR_PALETTE_OFFSET_POS = 15
    private const val ZSPR_PALETTE_LENGTH_POS = 19
    private const val ZSPR_MIN_HEADER_SIZE    = 21

    private val ZSPR_MAGIC = byteArrayOf(0x5A, 0x53, 0x50, 0x52) // "ZSPR"

    /**
     * Applies a sprite file to the ROM byte array (in-place modification).
     * Returns null on success, or an error string.
     */
    fun apply(spritePath: String, rom: ByteArray): String? {
        try {
            val file = File(spritePath)
            if (!file.exists()) return "Sprite file not found."

            val ext = file.extension.lowercase()
            val sprite = file.readBytes()

            when (ext) {
                "zspr" -> {
                    if (sprite.size < ZSPR_MIN_HEADER_SIZE)
                        return "ZSPR file is too small to contain a valid header."

                    // Verify magic
                    for (i in 0 until 4) {
                        if (sprite[i] != ZSPR_MAGIC[i])
                            return "File does not have a valid ZSPR header (wrong magic bytes)."
                    }

                    // Parse header fields (little-endian)
                    val gfxOffset  = readUInt32LE(sprite, ZSPR_GFX_OFFSET_POS)
                    val gfxLength  = readUInt16LE(sprite, ZSPR_GFX_LENGTH_POS)
                    val palOffset  = readUInt32LE(sprite, ZSPR_PALETTE_OFFSET_POS)
                    val palLength  = readUInt16LE(sprite, ZSPR_PALETTE_LENGTH_POS)

                    // Validate bounds within sprite file
                    if (gfxOffset + gfxLength > sprite.size)
                        return "ZSPR pixel data region exceeds file size."
                    if (palOffset + palLength > sprite.size)
                        return "ZSPR palette data region exceeds file size."
                    if (gfxLength > ROM_GFX_MAX_LENGTH)
                        return "ZSPR pixel data length ($gfxLength) exceeds maximum ($ROM_GFX_MAX_LENGTH)."

                    // Validate ROM bounds
                    if (ROM_GFX_OFFSET + gfxLength > rom.size)
                        return "ROM is too small to receive sprite pixel data."
                    if (palLength >= 4 && ROM_PALETTE_OFFSET + (palLength - 4) > rom.size)
                        return "ROM is too small to receive sprite palette data."
                    if (palLength >= 4 && ROM_GLOVES_OFFSET + 4 > rom.size)
                        return "ROM is too small to receive gloves palette data."

                    // Write pixel data
                    System.arraycopy(sprite, gfxOffset, rom, ROM_GFX_OFFSET, gfxLength)

                    // Write palette data (last 4 bytes are gloves)
                    if (palLength >= 4) {
                        val mainPalLength = palLength - 4
                        if (mainPalLength > 0)
                            System.arraycopy(sprite, palOffset, rom, ROM_PALETTE_OFFSET, mainPalLength)
                        // Gloves are the last 4 bytes of the palette block
                        System.arraycopy(sprite, palOffset + mainPalLength, rom, ROM_GLOVES_OFFSET, 4)
                    } else if (palLength > 0) {
                        System.arraycopy(sprite, palOffset, rom, ROM_PALETTE_OFFSET, palLength)
                    }
                }
                "spr" -> {
                    if (sprite.size < ROM_GFX_MAX_LENGTH)
                        return "Legacy .spr file must be at least 0x7000 ($ROM_GFX_MAX_LENGTH) bytes."
                    if (ROM_GFX_OFFSET + ROM_GFX_MAX_LENGTH > rom.size)
                        return "ROM is too small to receive sprite pixel data."

                    System.arraycopy(sprite, 0, rom, ROM_GFX_OFFSET, ROM_GFX_MAX_LENGTH)
                }
                else -> return "Unsupported sprite format. Please use .zspr or .spr files."
            }

            return null // success
        } catch (e: Exception) {
            return "Failed to apply sprite: ${e.message}"
        }
    }

    private fun readUInt16LE(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8)

    private fun readUInt32LE(data: ByteArray, offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)
}
