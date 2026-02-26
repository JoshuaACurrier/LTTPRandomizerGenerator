package com.lttprandomizer

import java.io.IOException
import java.util.zip.CRC32

object BpsPatcher {
    /**
     * Applies the alttpr.com two-stage patch to [sourceRom] and returns the patched ROM bytes.
     */
    fun apply(
        sourceRom: ByteArray,
        bpsPatch: ByteArray,
        dictPatches: List<Map<String, List<Int>>>,
        targetSizeMb: Int,
    ): ByteArray {
        var rom = applyBps(sourceRom, bpsPatch)
        rom = expandIfNeeded(rom, targetSizeMb)
        applyDictPatches(rom, dictPatches)
        writeChecksum(rom)
        return rom
    }

    // ── BPS ───────────────────────────────────────────────────────────────────

    private fun applyBps(source: ByteArray, patch: ByteArray): ByteArray {
        if (patch.size < 4 || patch[0] != 'B'.code.toByte() || patch[1] != 'P'.code.toByte() ||
            patch[2] != 'S'.code.toByte() || patch[3] != '1'.code.toByte()
        ) throw IOException("Invalid BPS patch: missing \"BPS1\" magic.")

        var pos = 4

        fun readVli(): Long {
            var value = 0L; var shift = 1L
            while (true) {
                if (pos >= patch.size) throw IOException("BPS patch truncated during VLI read.")
                val b = patch[pos++].toInt() and 0xFF
                value += (b and 0x7F) * shift
                if (b and 0x80 != 0) break
                shift = shift shl 7; value += shift
            }
            return value
        }

        readVli() // sourceSize (unused; we trust the caller's sourceRom)
        val targetSize = readVli()
        val metaLen = readVli()
        pos += metaLen.toInt()

        val output = ByteArray(targetSize.toInt())
        var outPos = 0
        var cpySrc = 0
        var cpyOut = 0
        val footerStart = patch.size - 12

        while (pos < footerStart) {
            val action = readVli()
            val length = (action shr 2).toInt() + 1
            val type   = (action and 3).toInt()

            when (type) {
                0 -> { // SOURCE_READ
                    System.arraycopy(source, outPos, output, outPos, length.coerceAtMost(source.size - outPos))
                    outPos += length
                }
                1 -> { // TARGET_READ
                    repeat(length) { output[outPos++] = patch[pos++] }
                }
                2 -> { // SOURCE_COPY
                    val d = readVli()
                    val delta = (d shr 1).toInt()
                    cpySrc += if (d and 1L != 0L) -delta else delta
                    repeat(length) { output[outPos++] = source[cpySrc++] }
                }
                3 -> { // TARGET_COPY
                    val d = readVli()
                    val delta = (d shr 1).toInt()
                    cpyOut += if (d and 1L != 0L) -delta else delta
                    repeat(length) { output[outPos++] = output[cpyOut++] }
                }
            }
        }

        // Verify patch CRC32
        val patchCrc = bytesToUInt32Le(patch, patch.size - 4)
        val computed = crc32(patch, 0, patch.size - 4)
        if (computed != patchCrc)
            throw IOException("BPS patch CRC32 mismatch. Corrupt download?")

        return output
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun expandIfNeeded(rom: ByteArray, targetSizeMb: Int): ByteArray {
        val targetBytes = targetSizeMb * 1024 * 1024
        if (rom.size >= targetBytes) return rom
        return rom.copyOf(targetBytes)
    }

    private fun applyDictPatches(rom: ByteArray, patches: List<Map<String, List<Int>>>) {
        for (entry in patches) {
            for ((offsetStr, values) in entry) {
                val offset = offsetStr.toInt()
                values.forEachIndexed { i, v -> rom[offset + i] = v.toByte() }
            }
        }
    }

    private fun writeChecksum(rom: ByteArray) {
        rom[0x7FDC] = 0; rom[0x7FDD] = 0; rom[0x7FDE] = 0; rom[0x7FDF] = 0
        var sum = 0L
        for (b in rom) sum += (b.toInt() and 0xFF)
        val checksum   = (sum and 0xFFFF).toInt()
        val complement = checksum xor 0xFFFF
        rom[0x7FDC] = (complement and 0xFF).toByte()
        rom[0x7FDD] = (complement shr 8).toByte()
        rom[0x7FDE] = (checksum and 0xFF).toByte()
        rom[0x7FDF] = (checksum shr 8).toByte()
    }

    private fun crc32(data: ByteArray, offset: Int, length: Int): Long {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value
    }

    private fun bytesToUInt32Le(data: ByteArray, offset: Int): Long =
        (data[offset].toLong() and 0xFF) or
        ((data[offset + 1].toLong() and 0xFF) shl 8) or
        ((data[offset + 2].toLong() and 0xFF) shl 16) or
        ((data[offset + 3].toLong() and 0xFF) shl 24)
}
