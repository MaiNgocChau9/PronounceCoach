package com.openpronounce.android.scoring

data class PhoneError(
    val expected: String,
    val heard: String,
    val confidence: Float,
    val isCorrect: Boolean
) {
    val displayExpected: String get() = expected.ifEmpty { "∅" }
    val displayHeard: String get() = heard.ifEmpty { "∅" }
}

data class WordError(
    val word: String,
    val position: Int,
    val expectedIpa: String,
    val heardIpa: String,
    val confidence: Float,
    val phoneErrors: List<PhoneError>
) {
    val isCorrect: Boolean get() = confidence > 0.6f
    val accuracyPercent: Int get() = (confidence * 100).toInt()
}

data class PronunciationResult(
    val score: Float,
    val expectedIpa: String,
    val heardIpa: String,
    val wordErrors: List<WordError>,
    val phonemeAccuracy: Float,
    val wordAccuracy: Float,
    val acousticDistance: Float,
    val feedback: String
) {
    val overallPercent: Int get() = score.toInt().coerceIn(0, 100)
    val hasErrors: Boolean get() = wordErrors.any { !it.isCorrect }

    companion object {
        fun empty() = PronunciationResult(
            score = 0f,
            expectedIpa = "",
            heardIpa = "",
            wordErrors = emptyList(),
            phonemeAccuracy = 0f,
            wordAccuracy = 0f,
            acousticDistance = 0f,
            feedback = "No audio recorded"
        )
    }
}
