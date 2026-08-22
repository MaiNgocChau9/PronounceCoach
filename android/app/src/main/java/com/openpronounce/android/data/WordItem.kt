package com.openpronounce.android.data

import kotlinx.serialization.Serializable

@Serializable
data class WordItem(
    val word: String,
    val ipa: String,
    val meaning: String = "",
    val example: String = "",
    val category: String = "general",
    val difficulty: Int = 1 // 1=easy, 2=medium, 3=hard
)

@Serializable
data class WordCategory(
    val id: String,
    val name: String,
    val words: List<WordItem>
)
