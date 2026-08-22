package com.openpronounce.android.scoring

object PronunciationScorer {

    private const val ACOUSTIC_WEIGHT = 0.3f
    private const val PHONEME_WEIGHT = 0.4f
    private const val WORD_WEIGHT = 0.3f
    private const val ACOUSTIC_GOOD_BASELINE = 6.0f
    private const val ACOUSTIC_GOOD_RANGE = 9.0f

    fun score(
        expectedPhones: List<String>,
        heardPhones: List<String>,
        ctcConfidences: List<Float> = emptyList(),
        acousticDistance: Float = 0f
    ): PronunciationResult {
        if (expectedPhones.isEmpty()) {
            return PronunciationResult.empty()
        }

        // 1. Align and compute phone-level errors
        val wordError = ConfidenceScorer.scoreWord(expectedPhones, heardPhones, ctcConfidences)

        // 2. Compute phoneme error rate
        val phonemeErrorRate = LevenshteinAligner.errorRate(expectedPhones, heardPhones)

        // 3. Compute word error rate (single word context)
        val wordErrorRate = if (wordError.confidence < 1f) 1f - wordError.confidence else 0f

        // 4. Compute acoustic score
        val acousticScore = computeAcousticScore(acousticDistance)

        // 5. Compute component scores
        val phonemeScore = (1f - phonemeErrorRate) * 100f
        val wordScore = (1f - wordErrorRate) * 100f

        // 6. Final weighted score
        val finalScore = (
            ACOUSTIC_WEIGHT * acousticScore +
            PHONEME_WEIGHT * phonemeScore +
            WORD_WEIGHT * wordScore
        ).coerceIn(0f, 100f)

        // 7. Generate feedback
        val feedback = generateFeedback(finalScore, wordError.phoneErrors)

        return PronunciationResult(
            score = finalScore,
            expectedIpa = expectedPhones.joinToString(" "),
            heardIpa = heardPhones.joinToString(" "),
            wordErrors = listOf(wordError.copy(word = expectedPhones.joinToString(" "))),
            phonemeAccuracy = (1f - phonemeErrorRate).coerceIn(0f, 1f),
            wordAccuracy = (1f - wordErrorRate).coerceIn(0f, 1f),
            acousticDistance = acousticDistance,
            feedback = feedback
        )
    }

    private fun computeAcousticScore(distance: Float): Float {
        return ((ACOUSTIC_GOOD_BASELINE + ACOUSTIC_GOOD_RANGE - distance) /
                ACOUSTIC_GOOD_RANGE * 100f).coerceIn(0f, 100f)
    }

    private fun generateFeedback(score: Float, phoneErrors: List<PhoneError>): String {
        val sb = StringBuilder()

        when {
            score >= 90 -> sb.append("Excellent pronunciation!")
            score >= 75 -> sb.append("Good job! A few minor issues.")
            score >= 50 -> sb.append("Decent attempt. Keep practicing!")
            else -> sb.append("Keep trying! Practice makes perfect.")
        }

        val incorrectPhones = phoneErrors.filter { !it.isCorrect }
        if (incorrectPhones.isNotEmpty()) {
            sb.append("\n\nSounds to improve:")
            for (error in incorrectPhones.take(3)) {
                sb.append("\n• /${error.expected}/ → you said /${error.displayHeard}/")
            }
        }

        return sb.toString()
    }
}
