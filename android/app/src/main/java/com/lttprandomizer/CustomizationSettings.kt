package com.lttprandomizer

import kotlinx.serialization.Serializable

@Serializable
data class CustomizationSettings(
    val heartBeepSpeed: String = "normal",
    val heartColor: String     = "red",
    val menuSpeed: String      = "normal",
    val quickSwap: String      = "off",
)

object CustomizationOptions {
    val heartBeepSpeed = listOf(
        DropdownOption("Normal",  "normal"),
        DropdownOption("Off",     "off"),
        DropdownOption("Double",  "double"),
        DropdownOption("Half",    "half"),
        DropdownOption("Quarter", "quarter"),
    )
    val heartColor = listOf(
        DropdownOption("Red",    "red"),
        DropdownOption("Blue",   "blue"),
        DropdownOption("Green",  "green"),
        DropdownOption("Yellow", "yellow"),
    )
    val menuSpeed = listOf(
        DropdownOption("Normal",  "normal"),
        DropdownOption("Half",    "half"),
        DropdownOption("Double",  "double"),
        DropdownOption("Triple",  "triple"),
        DropdownOption("Quad",    "quad"),
        DropdownOption("Instant", "instant"),
    )
    val quickSwap = listOf(
        DropdownOption("Off", "off"),
        DropdownOption("On",  "on"),
    )
}
