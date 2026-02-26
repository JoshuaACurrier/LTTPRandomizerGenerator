package com.lttprandomizer

import android.content.Context
import android.net.Uri
import java.util.zip.CRC32

object RomValidator {
    private const val EXPECTED_SIZE = 1_048_576L  // 1 MB, headerless
    private const val EXPECTED_CRC32 = 0x777AAC3FL
    private const val COPIER_HEADER_SIZE = 512

    /**
     * Returns null if valid and sets [romBytes], or returns an error message.
     */
    fun validate(context: Context, uri: Uri): Pair<String?, ByteArray> {
        val raw = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Pair("Cannot open ROM file.", ByteArray(0))
        } catch (e: Exception) {
            return Pair("Cannot read ROM: ${e.message}", ByteArray(0))
        }

        // Strip 512-byte copier header if present
        val bytes = if (raw.size == (EXPECTED_SIZE + COPIER_HEADER_SIZE).toInt()) {
            raw.copyOfRange(COPIER_HEADER_SIZE, raw.size)
        } else raw

        if (bytes.size.toLong() != EXPECTED_SIZE) {
            return Pair(
                "ROM size is ${bytes.size} bytes. Expected ${EXPECTED_SIZE} bytes " +
                "(Japanese v1.0 ALttP, headerless).",
                ByteArray(0)
            )
        }

        val crc = CRC32().also { it.update(bytes) }.value
        if (crc != EXPECTED_CRC32) {
            return Pair(
                "ROM CRC32 mismatch (got 0x${crc.toString(16).uppercase()}, " +
                "expected 0x${EXPECTED_CRC32.toString(16).uppercase()}).\n" +
                "This randomizer requires the Japanese v1.0 ALttP ROM.",
                ByteArray(0)
            )
        }

        return Pair(null, bytes)
    }
}
