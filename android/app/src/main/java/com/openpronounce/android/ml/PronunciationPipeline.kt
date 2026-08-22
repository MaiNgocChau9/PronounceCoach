package com.openpronounce.android.ml

import android.content.Context
import android.util.Log
import com.openpronounce.android.scoring.PronunciationResult
import com.openpronounce.android.scoring.PronunciationScorer
import com.openpronounce.android.data.WordItem

class PronunciationPipeline(private val context: Context) {

    companion object {
        private const val TAG = "PronunciationPipeline"
    }

    private val recognizer = PhonemeRecognizer(context)

    val isReady: Boolean get() = recognizer.isReady

    fun loadModels(): Result<Unit> {
        Log.i(TAG, "Loading models...")
        return recognizer.load(
            modelPath = "models/wav2vec2_phoneme.onnx",
            vocabPath = "models/vocab.txt"
        )
    }

    fun analyze(
        audioData: FloatArray,
        word: WordItem,
        sampleRate: Int = 16000
    ): PronunciationResult {
        if (!isReady) {
            Log.w(TAG, "Model not ready, returning empty result")
            return PronunciationResult(
                score = 0f,
                expectedIpa = word.ipa,
                heardIpa = "",
                wordErrors = emptyList(),
                phonemeAccuracy = 0f,
                wordAccuracy = 0f,
                acousticDistance = 0f,
                feedback = "Model not loaded. Please restart the app."
            )
        }

        Log.i(TAG, "Analyzing audio for word: ${word.word}")

        // 1. Transcribe audio to text
        val transcribed = recognizer.transcribe(audioData, sampleRate)
        Log.i(TAG, "Transcribed: '$transcribed'")

        // 2. Compare with expected word
        val expected = word.word.lowercase().trim()
        val heard = transcribed.lowercase().trim()

        // 3. Compute similarity
        val similarity = computeSimilarity(expected, heard)
        val score = (similarity * 100f).coerceIn(0f, 100f)

        // 4. Generate feedback
        val feedback = generateFeedback(expected, heard, score)

        return PronunciationResult(
            score = score,
            expectedIpa = word.ipa.ifEmpty { expected },
            heardIpa = heard,
            wordErrors = emptyList(),
            phonemeAccuracy = similarity,
            wordAccuracy = if (expected == heard) 1f else similarity * 0.8f,
            acousticDistance = 0f,
            feedback = feedback
        )
    }

    private fun computeSimilarity(expected: String, heard: String): Float {
        if (expected.isEmpty() || heard.isEmpty()) return 0f
        if (expected == heard) return 1f

        // Character-level similarity using Levenshtein
        val dist = levenshteinDistance(expected, heard)
        val maxLen = maxOf(expected.length, heard.length)
        return (1f - dist.toFloat() / maxLen).coerceIn(0f, 1f)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[m][n]
    }

    private fun generateFeedback(expected: String, heard: String, score: Float): String {
        if (heard.isEmpty()) {
            return "No speech detected. Please try again.\nExpected: \"$expected\""
        }

        val sb = StringBuilder()
        when {
            score >= 0.9f -> sb.append("Excellent! Perfect pronunciation!")
            score >= 0.7f -> sb.append("Good job! Almost there.")
            score >= 0.5f -> sb.append("Not bad, but needs improvement.")
            else -> sb.append("Keep practicing!")
        }

        sb.append("\n\nExpected: \"$expected\"")
        sb.append("\nYou said: \"$heard\"")

        if (score < 0.7f) {
            sb.append("\n\nTip: Listen carefully and try again.")
        }

        return sb.toString()
    }

    fun close() {
        recognizer.close()
    }
}
