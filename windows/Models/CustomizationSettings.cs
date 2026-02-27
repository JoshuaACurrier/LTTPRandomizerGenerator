namespace LTTPRandomizerGenerator.Models
{
    /// <summary>
    /// Cosmetic settings applied to the ROM after BPS patching.
    /// These bytes are written to the SNES expansion area (0x18xxxx) and
    /// are NOT sent to the alttpr.com API.
    /// </summary>
    public class CustomizationSettings
    {
        public string HeartBeepSpeed { get; set; } = "normal";
        public string HeartColor     { get; set; } = "red";
        public string MenuSpeed      { get; set; } = "normal";
        public string QuickSwap      { get; set; } = "off";
    }

    public static class CustomizationOptions
    {
        public static readonly DropdownOption[] HeartBeepSpeed =
        [
            new("Normal",   "normal"),
            new("Double",   "double"),
            new("Half",     "half"),
            new("Quarter",  "quarter"),
            new("Off",      "off"),
        ];

        public static readonly DropdownOption[] HeartColor =
        [
            new("Red",    "red"),
            new("Blue",   "blue"),
            new("Green",  "green"),
            new("Yellow", "yellow"),
        ];

        public static readonly DropdownOption[] MenuSpeed =
        [
            new("Normal",  "normal"),
            new("Half",    "half"),
            new("Double",  "double"),
            new("Triple",  "triple"),
            new("Quad",    "quad"),
            new("Instant", "instant"),
        ];

        public static readonly DropdownOption[] QuickSwap =
        [
            new("Off", "off"),
            new("On",  "on"),
        ];
    }
}
