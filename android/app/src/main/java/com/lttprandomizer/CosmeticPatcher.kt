package com.lttprandomizer

/**
 * Applies cosmetic ROM patches to the expansion area (0x18xxxx) after BPS patching.
 * Must be called AFTER BpsPatcher.apply() so the ROM has been expanded to 2 MB.
 * Recalculates the SNES HiROM checksum after writing.
 */
object CosmeticPatcher {

    // ROM addresses in the SNES expansion area (valid after BPS expansion to 2 MB)
    private const val ADDR_HEART_BEEP  = 0x180033
    private const val ADDR_HEART_COLOR = 0x187020
    private const val ADDR_MENU_SPEED  = 0x180048
    private const val ADDR_QUICK_SWAP  = 0x18004B

    /**
     * Writes cosmetic bytes into the ROM and recalculates the SNES checksum.
     * Modifies [rom] in place and returns the same array for chaining.
     */
    fun apply(rom: ByteArray, s: CustomizationSettings): ByteArray {
        rom[ADDR_HEART_BEEP] = when (s.heartBeepSpeed) {
            "off"     -> 0x00
            "double"  -> 0x10
            "half"    -> 0x40
            "quarter" -> 0x80
            else      -> 0x20  // normal
        }.toByte()

        rom[ADDR_HEART_COLOR] = when (s.heartColor) {
            "blue"   -> 0x01
            "green"  -> 0x02
            "yellow" -> 0x03
            else     -> 0x00  // red
        }.toByte()

        rom[ADDR_MENU_SPEED] = when (s.menuSpeed) {
            "half"    -> 0x04
            "double"  -> 0x10
            "triple"  -> 0x18
            "quad"    -> 0x20
            "instant" -> 0xE8
            else      -> 0x08  // normal
        }.toByte()

        rom[ADDR_QUICK_SWAP] = (if (s.quickSwap == "on") 0x01 else 0x00).toByte()

        RomUtils.writeChecksum(rom)
        return rom
    }
}
