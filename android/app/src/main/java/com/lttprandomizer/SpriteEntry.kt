package com.lttprandomizer

import kotlinx.serialization.Serializable

@Serializable
data class SpriteEntry(
    val name: String = "",
    val author: String = "",
    val file: String = "",
    val preview: String = "",
    val tags: List<String> = emptyList(),
    val usage: List<String> = emptyList(),
)
