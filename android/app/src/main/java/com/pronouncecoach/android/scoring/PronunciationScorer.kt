package com.pronouncecoach.android.scoring

/**
 * One target word of the practice text: its display form, its normalized phones and the
 * index of its first phone inside the full expected sequence.
 */
data class TargetWord(
    val text: String,
    val phones: List<String>,
    val startIndex: Int
)

object PronunciationScorer {

    // No acoustic model on-device, so the whole weight goes to what we can actually
    // measure (the Python backend splits 0.3 acoustic / 0.4 phones / 0.3 words; keeping
    // an acoustic term here would hand every attempt 30 free points).
    private const val PHONEME_WEIGHT = 0.6f
    private const val WORD_WEIGHT = 0.4f

    /** A word counts as mispronounced below this per-word percent. */
    private const val WORD_PASS_PERCENT = 60

    fun score(
        expectedPhones: List<String>,
        heardPhones: List<String>,
        targetWords: List<TargetWord> = emptyList()
    ): PronunciationResult {
        if (expectedPhones.isEmpty()) {
            return PronunciationResult.empty()
        }

        // Silence / unintelligible audio: never invent a score for it.
        if (heardPhones.isEmpty()) {
            return PronunciationResult.empty(noSpeech = true)
        }

        // 1. Global phone-level alignment.
        val ops = LevenshteinAligner.align(expectedPhones, heardPhones)
        val phonemeErrorRate = LevenshteinAligner.errorRate(expectedPhones, heardPhones)

        // 2. Per-word breakdown from the global alignment.
        val words = if (targetWords.isNotEmpty()) {
            scoreWords(expectedPhones, heardPhones, ops, targetWords)
        } else {
            listOf(
                WordError(
                    word = "",
                    position = 0,
                    expectedIpa = expectedPhones.joinToString(" "),
                    heardIpa = heardPhones.joinToString(" "),
                    confidence = (1f - phonemeErrorRate).coerceIn(0f, 1f),
                    phoneErrors = collectPhoneErrors(expectedPhones, heardPhones, ops)
                )
            )
        }

        // 3. Overall score: phoneme accuracy + share of words pronounced correctly.
        val phonemeScore = ((1f - phonemeErrorRate) * 100f).coerceIn(0f, 100f)
        val wordScore = words.count { it.accuracyPercent >= WORD_PASS_PERCENT } * 100f / words.size
        val finalScore = (PHONEME_WEIGHT * phonemeScore + WORD_WEIGHT * wordScore).coerceIn(0f, 100f)

        val feedback = generateFeedback(finalScore)

        return PronunciationResult(
            score = finalScore,
            expectedIpa = expectedPhones.joinToString(" "),
            heardIpa = heardPhones.joinToString(" "),
            wordErrors = words,
            phonemeAccuracy = (1f - phonemeErrorRate).coerceIn(0f, 1f),
            wordAccuracy = wordScore / 100f,
            acousticDistance = 0f,
            feedback = feedback
        )
    }

    /** Per-word percent + the exact phone errors of each word, from the global alignment. */
    private fun scoreWords(
        expectedPhones: List<String>,
        heardPhones: List<String>,
        ops: List<LevenshteinAligner.EditOp>,
        targets: List<TargetWord>
    ): List<WordError> {
        return targets.map { target ->
            val end = target.startIndex + target.phones.size

            var cost = 0f
            val phoneErrors = mutableListOf<PhoneError>()
            val heardParts = linkedSetOf<String>()

            for (op in ops) {
                when (op.type) {
                    "sub" -> if (op.i in target.startIndex until end) {
                        val exp = expectedPhones[op.i]
                        val heard = heardPhones.getOrNull(op.j) ?: ""
                        val err = ConfidenceScorer.scorePhone(exp, heard)
                        phoneErrors.add(err)
                        cost += 1f - err.confidence   // near-phone subs cost half
                        if (heard.isNotEmpty()) heardParts.add(heard)
                    }
                    "del" -> if (op.i in target.startIndex until end) {
                        phoneErrors.add(PhoneError(expectedPhones[op.i], "", 0f, isCorrect = false))
                        cost += 1f
                    }
                    "ins" -> {
                        // Extra sound: charge it to the nearest preceding target word.
                        val owner = targets.lastOrNull { it.startIndex <= op.i.coerceAtLeast(0) }
                        if (owner === target) {
                            cost += 0.5f
                            heardPhones.getOrNull(op.j)?.let { heardParts.add(it) }
                        }
                    }
                }
            }

            // Per-phone correctness + what was heard at each position, for the UI table.
            val wrongPhoneIndices = mutableSetOf<Int>()
            val heardByIndex = mutableMapOf<Int, String>()
            ops.forEach { op ->
                when (op.type) {
                    "sub" -> if (op.i in target.startIndex until end) {
                        wrongPhoneIndices.add(op.i - target.startIndex)
                        heardByIndex[op.i - target.startIndex] = heardPhones.getOrNull(op.j) ?: ""
                    }
                    "del" -> if (op.i in target.startIndex until end) {
                        wrongPhoneIndices.add(op.i - target.startIndex)
                    }
                }
            }

            val percent = (((target.phones.size - cost) / target.phones.size) * 100f)
                .coerceIn(0f, 100f)

            WordError(
                word = target.text,
                position = target.startIndex,
                expectedIpa = target.phones.joinToString(" "),
                heardIpa = heardParts.joinToString(" "),
                confidence = percent / 100f,
                phoneErrors = phoneErrors,
                phoneCorrect = target.phones.indices.map { it !in wrongPhoneIndices },
                phoneHeard = target.phones.indices.map { heardByIndex[it] ?: "" }
            )
        }
    }

    private fun collectPhoneErrors(
        expectedPhones: List<String>,
        heardPhones: List<String>,
        ops: List<LevenshteinAligner.EditOp>
    ): List<PhoneError> = ops.mapNotNull { op ->
        when (op.type) {
            "sub" -> ConfidenceScorer.scorePhone(
                expectedPhones[op.i], heardPhones.getOrNull(op.j) ?: ""
            )
            "del" -> PhoneError(expectedPhones[op.i], "", 0f, isCorrect = false)
            else -> null
        }
    }

    private fun generateFeedback(score: Float): String = when {
        score >= 90 -> "Excellent pronunciation!"
        score >= 75 -> "Great job! Just a few sounds to polish."
        score >= 50 -> "Almost there — check the highlighted words."
        else -> "Keep practicing! Tap any red word to hear it."
    }
}
