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
    val phoneErrors: List<PhoneError>,
    /** Per-phone correctness, aligned with expectedIpa tokens; empty when unknown. */
    val phoneCorrect: List<Boolean> = emptyList(),
    /** What was heard at each phone position ("" when correct or missing). */
    val phoneHeard: List<String> = emptyList()
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
    val feedback: String,
    /** True when the recording was silence / too quiet to score at all. */
    val noSpeechDetected: Boolean = false
) {
    val overallPercent: Int get() = score.toInt().coerceIn(0, 100)
    val hasErrors: Boolean get() = wordErrors.any { !it.isCorrect }

    companion object {
        fun empty(noSpeech: Boolean = false) = PronunciationResult(
            score = 0f,
            expectedIpa = "",
            heardIpa = "",
            wordErrors = emptyList(),
            phonemeAccuracy = 0f,
            wordAccuracy = 0f,
            acousticDistance = 0f,
            feedback = if (noSpeech) "We couldn't hear you clearly. Try again a bit louder."
                       else "No audio recorded",
            noSpeechDetected = noSpeech
        )
    }
}
