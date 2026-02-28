package com.lttprandomizer

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile

object EsDeHelper {

    private const val FOLDER_NAME = "lttpr"
    private const val GAMELIST_NAME = "gamelist"
    private const val INFO_NAME = "_info"

    /**
     * Ensures the lttpr/ subfolder exists inside the given tree, returns it.
     */
    fun ensureFolder(docTree: DocumentFile): DocumentFile {
        return docTree.findFile(FOLDER_NAME)
            ?: docTree.createDirectory(FOLDER_NAME)
            ?: throw IllegalStateException("Cannot create $FOLDER_NAME subfolder in output folder.")
    }

    /**
     * Appends a <game> entry to gamelist.xml inside the given folder.
     * Creates gamelist.xml if it doesn't exist.
     */
    fun updateGamelist(
        resolver: ContentResolver,
        folder: DocumentFile,
        romFileName: String,
        hash: String,
        permalink: String,
    ) {
        val existingFile = folder.findFile("$GAMELIST_NAME.xml")
        val existingXml = if (existingFile != null) {
            resolver.openInputStream(existingFile.uri)?.use { it.bufferedReader().readText() }
        } else null

        val gameEntry = buildGameEntry(romFileName, hash, permalink)

        val newXml = if (existingXml != null && existingXml.contains("<gameList>")) {
            // Insert before closing tag
            existingXml.replace("</gameList>", "$gameEntry\n</gameList>")
        } else {
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gameList>\n$gameEntry\n</gameList>\n"
        }

        val targetFile = existingFile
            ?: folder.createFile("text/xml", GAMELIST_NAME)
            ?: throw IllegalStateException("Cannot create gamelist.xml in output folder.")

        resolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
            out.write(newXml.toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Cannot write to gamelist.xml.")
    }

    /**
     * Writes _info.txt with ES-DE custom system setup instructions if it doesn't already exist.
     */
    fun writeInfoFile(resolver: ContentResolver, folder: DocumentFile) {
        if (folder.findFile("$INFO_NAME.txt") != null) return

        val file = folder.createFile("text/plain", INFO_NAME)
            ?: return // non-fatal if we can't create it

        resolver.openOutputStream(file.uri)?.use { out ->
            out.write(INFO_TEXT.toByteArray(Charsets.UTF_8))
        }
    }

    /**
     * Writes or appends the lttpr system entry to es_systems.xml in the given folder.
     * Returns null on success, or a status/error message.
     */
    fun writeEsSystems(resolver: ContentResolver, folder: DocumentFile): String? {
        return try {
            val existingFile = folder.findFile("es_systems.xml")
            val existingXml = if (existingFile != null) {
                resolver.openInputStream(existingFile.uri)?.use { it.bufferedReader().readText() }
            } else null

            if (existingXml != null && existingXml.contains("<name>lttpr</name>")) {
                return "already_configured"
            }

            val systemBlock = """  <system>
    <name>lttpr</name>
    <fullname>A Link to the Past Randomizer</fullname>
    <path>%ROMPATH%/lttpr</path>
    <extension>.sfc .SFC</extension>
    <command label="RetroArch (snes9x)">%EMULATOR_RETROARCH% -L %CORE_RETROARCH%/snes9x_libretro.so %ROM%</command>
    <platform>snes</platform>
    <theme>snes</theme>
  </system>"""

            val newXml = if (existingXml != null && existingXml.contains("<systemList>")) {
                existingXml.replace("</systemList>", "$systemBlock\n</systemList>")
            } else {
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<systemList>\n$systemBlock\n</systemList>\n"
            }

            val targetFile = existingFile
                ?: folder.createFile("text/xml", "es_systems")
                ?: return "Cannot create es_systems.xml in selected folder."

            resolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
                out.write(newXml.toByteArray(Charsets.UTF_8))
            } ?: return "Cannot write to es_systems.xml."

            null
        } catch (e: Exception) {
            "Error writing es_systems.xml: ${e.message}"
        }
    }

    private fun buildGameEntry(romFileName: String, hash: String, permalink: String): String {
        val now = java.text.SimpleDateFormat("yyyyMMdd'T'HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        return """  <game>
    <path>./$romFileName</path>
    <name>ALttP Randomizer - $hash</name>
    <desc>A Link to the Past Randomizer seed. Permalink: $permalink</desc>
    <rating>0</rating>
    <releasedate>$now</releasedate>
    <developer>alttpr.com</developer>
    <publisher>Community</publisher>
    <genre>Action-Adventure</genre>
  </game>"""
    }

    private val INFO_TEXT = """ES-DE Custom System Setup for LTTP Randomizer
==============================================

To add this folder as a custom system in ES-DE:

1. Open your ES-DE custom_systems folder:
   - Windows: %USERPROFILE%\.emulationstation\custom_systems\
   - Linux:   ~/.emulationstation/custom_systems/
   - Android: /storage/emulated/0/ES-DE/custom_systems/

2. Create or edit es_systems.xml and add:

<system>
    <name>lttpr</name>
    <fullname>A Link to the Past Randomizer</fullname>
    <path>%ROMPATH%/lttpr</path>
    <extension>.sfc .SFC</extension>
    <command label="RetroArch (snes9x)">%EMULATOR_RETROARCH% -L %CORE_RETROARCH%/snes9x_libretro.so %ROM%</command>
    <platform>snes</platform>
    <theme>snes</theme>
</system>

3. Move or symlink this lttpr folder into your ROMs root folder
4. Restart ES-DE - the system should appear using the SNES theme
"""
}
