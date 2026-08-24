package com.pronouncecoach.android.scoring

object ConfidenceScorer {

    const val NEAR_PHONE_COST = 0.5f
    const val FINAL_EXTRA_COST = 0.25f
    const val FINAL_DELETION_COST = 0.5f
    const val PHONE_ERROR_THRESHOLD = 0.4f
    const val PHONE_ERROR_MIN_EDITS = 2
    const val PHONE_PLAUSIBLE_POSTERIOR = 0.05f

    fun scorePhone(
        expected: String,
        heard: String,
        ctcPosterior: Float = 1f,
        isFinal: Boolean = false,
        isPlausible: Boolean = true
    ): PhoneError {
        if (expected == heard) {
            return PhoneError(expected, heard, 1f, isCorrect = true)
        }

        var confidence = 1f

        // Near-phone reduction
        if (PhoneNormalizer.isNearPhone(expected, heard)) {
            confidence *= NEAR_PHONE_COST
        }

        // Final position penalty
        if (isFinal) {
            confidence *= FINAL_EXTRA_COST
        }

        // Plausibility check
        if (!isPlausible) {
            confidence *= (ctcPosterior / PHONE_PLAUSIBLE_POSTERIOR).coerceIn(0f, 1f)
        }

        return PhoneError(
            expected = expected,
            heard = heard,
            confidence = confidence.coerceIn(0f, 1f),
            isCorrect = false
        )
    }

    fun scoreWord(
        expectedPhones: List<String>,
        heardPhones: List<String>,
        ctcConfidences: List<Float> = emptyList()
    ): WordError {
        val ops = LevenshteinAligner.align(expectedPhones, heardPhones)
        val phoneErrors = mutableListOf<PhoneError>()
        var totalConfidence = 0f

        // Map heard phones for lookup
        val heardList = heardPhones.toMutableList()

        for (op in ops) {
            when (op.type) {
                "sub" -> {
                    val exp = expectedPhones[op.i]
                    val heard = if (op.j >= 0 && op.j < heardList.size) heardList[op.j] else ""
                    val posterior = if (op.j >= 0 && op.j < ctcConfidences.size) ctcConfidences[op.j] else 1f
                    val error = scorePhone(exp, heard, posterior)
                    phoneErrors.add(error)
                    totalConfidence += error.confidence
                }
                "del" -> {
                    val exp = expectedPhones[op.i]
                    val error = PhoneError(exp, "", 0f, isCorrect = false)
                    phoneErrors.add(error)
                    totalConfidence += FINAL_DELETION_COST
                }
                "ins" -> {
                    val heard = if (op.j >= 0 && op.j < heardList.size) heardList[op.j] else ""
                    val error = PhoneError("", heard, 0.3f, isCorrect = false)
                    phoneErrors.add(error)
                }
            }
        }

        // Handle deletions at end of expected
        val alignedExpected = ops.filter { it.type != "ins" }.map { it.i }.toSet()
        for (i in expectedPhones.indices) {
            if (i !in alignedExpected) {
                val error = PhoneError(expectedPhones[i], "", 0f, isCorrect = false)
                phoneErrors.add(error)
                totalConfidence += FINAL_DELETION_COST
            }
        }

        val avgConfidence = if (phoneErrors.isNotEmpty()) {
            totalConfidence / phoneErrors.size
        } else {
            1f
        }

        val editCount = ops.count { it.type != "match" }
        val isWordError = avgConfidence < PHONE_ERROR_THRESHOLD ||
                editCount >= PHONE_ERROR_MIN_EDITS

        return WordError(
            word = "",
            position = 0,
            expectedIpa = expectedPhones.joinToString(" "),
            heardIpa = heardPhones.joinToString(" "),
            confidence = if (isWordError) avgConfidence.coerceIn(0f, 1f) else 1f,
            phoneErrors = phoneErrors
        )
    }
}
