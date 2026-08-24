package com.pronouncecoach.android.scoring

object PhoneNormalizer {

    private val cotCaughtMerger = mapOf(
        "ɔː" to "ɔ",
        "ɑː" to "ɑ"
    )

    private val flappingRules = mapOf(
        "ɾ" to "t",
        "ɾ̃" to "d"
    )

    private val vowelReductions = mapOf(
        "ə" to "ə",
        "ɪ" to "ɪ",
        "ʊ" to "ʊ"
    )

    private val lengthMarks = setOf("ː", "ˑ", "̆")

    fun normalize(phone: String): String {
        var result = phone

        // Remove length marks
        for (mark in lengthMarks) {
            result = result.replace(mark, "")
        }

        // Apply mergers
        cotCaughtMerger.forEach { (from, to) ->
            result = result.replace(from, to)
        }

        // Apply flapping
        flappingRules.forEach { (from, to) ->
            result = result.replace(from, to)
        }

        return result
    }

    fun normalizeSequence(phones: List<String>): List<String> {
        return phones
            .filter { it.isNotEmpty() && it != "<s>" && it != "</s>" && it != "<pad>" }
            .map { normalize(it) }
            .collapseRepeats()
    }

    private fun List<String>.collapseRepeats(): List<String> {
        if (isEmpty()) return emptyList()
        val result = mutableListOf(first())
        for (i in 1 until size) {
            if (this[i] != result.last()) {
                result.add(this[i])
            }
        }
        return result
    }

    // Near-phone detection for confidence scoring
    private val voicingPairs = setOf(
        "p" to "b", "t" to "d", "k" to "g",
        "f" to "v", "θ" to "ð", "s" to "z",
        "ʃ" to "ʒ", "tʃ" to "dʒ"
    )

    private val tenseLaxPairs = setOf(
        "i" to "ɪ", "u" to "ʊ",
        "e" to "ɛ", "o" to "ɔ",
        "æ" to "ɑ"
    )

    fun isNearPhone(a: String, b: String): Boolean {
        if (a == b) return true
        val normalizedA = normalize(a)
        val normalizedB = normalize(b)
        if (normalizedA == normalizedB) return true

        return voicingPairs.any { (p, q) ->
            (normalizedA == p && normalizedB == q) ||
            (normalizedA == q && normalizedB == p)
        } || tenseLaxPairs.any { (p, q) ->
            (normalizedA == p && normalizedB == q) ||
            (normalizedA == q && normalizedB == p)
        }
    }
}
