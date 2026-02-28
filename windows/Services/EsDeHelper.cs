using System;
using System.IO;
using System.Xml.Linq;

namespace LTTPRandomizerGenerator.Services
{
    public static class EsDeHelper
    {
        private const string FolderName = "lttpr";
        private const string GamelistFile = "gamelist.xml";
        private const string InfoFile = "_info.txt";

        /// <summary>
        /// Ensures the lttpr\ subfolder exists inside the given output folder. Returns the full path.
        /// </summary>
        public static string EnsureFolder(string outputFolder)
        {
            string path = Path.Combine(outputFolder, FolderName);
            Directory.CreateDirectory(path);
            return path;
        }

        /// <summary>
        /// Appends a game entry to gamelist.xml inside the given folder.
        /// Creates the file if it doesn't exist.
        /// </summary>
        public static void UpdateGamelist(string folder, string romFileName, string hash, string permalink)
        {
            string gamelistPath = Path.Combine(folder, GamelistFile);
            XDocument doc;

            if (File.Exists(gamelistPath))
            {
                doc = XDocument.Load(gamelistPath);
                if (doc.Root?.Name != "gameList")
                    doc = new XDocument(new XElement("gameList"));
            }
            else
            {
                doc = new XDocument(new XDeclaration("1.0", "UTF-8", null),
                    new XElement("gameList"));
            }

            string now = DateTime.Now.ToString("yyyyMMdd'T'HHmmss");

            var game = new XElement("game",
                new XElement("path", $"./{romFileName}"),
                new XElement("name", $"ALttP Randomizer - {hash}"),
                new XElement("desc", $"A Link to the Past Randomizer seed. Permalink: {permalink}"),
                new XElement("rating", "0"),
                new XElement("releasedate", now),
                new XElement("developer", "alttpr.com"),
                new XElement("publisher", "Community"),
                new XElement("genre", "Action-Adventure")
            );

            doc.Root!.Add(game);
            doc.Save(gamelistPath);
        }

        /// <summary>
        /// Writes _info.txt with ES-DE custom system setup instructions if it doesn't already exist.
        /// </summary>
        public static void WriteInfoFile(string folder)
        {
            string infoPath = Path.Combine(folder, InfoFile);
            if (File.Exists(infoPath)) return;

            File.WriteAllText(infoPath, InfoText);
        }

        private const string InfoText = @"ES-DE Custom System Setup for LTTP Randomizer
==============================================

To add this folder as a custom system in ES-DE:

1. Open your ES-DE custom_systems folder:
   - Windows: %USERPROFILE%\.emulationstation\custom_systems\
   - Linux:   ~/.emulationstation/custom_systems/

2. Create or edit es_systems.xml and add:

<system>
    <name>lttpr</name>
    <fullname>A Link to the Past Randomizer</fullname>
    <path>%ROMPATH%/lttpr</path>
    <extension>.sfc .SFC</extension>
    <command label=""RetroArch (snes9x)"">%EMULATOR_RETROARCH% -L %CORE_RETROARCH%/snes9x_libretro.so %ROM%</command>
    <platform>snes</platform>
    <theme>snes</theme>
</system>

3. Move or symlink this lttpr folder into your ROMs root folder
4. Restart ES-DE - the system should appear using the SNES theme
";
    }
}
