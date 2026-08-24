package com.pronouncecoach.android.ml

import android.content.Context
import android.util.Log
import com.pronouncecoach.android.scoring.PhoneNormalizer
import com.pronouncecoach.android.scoring.PronunciationResult
import com.pronouncecoach.android.scoring.PronunciationScorer

/**
 * Pronunciation assessment mirroring the Python pipeline's word path:
 * the bundled ONNX model is wav2vec2-large-960h (a LETTER CTC model), so the recording
 * is first transcribed to text, the transcription is converted to espeak IPA phones via
 * the bundled G2P lexicon, and only then aligned with the expected phones — both sides
 * in the same IPA convention, like compare_transcriptions() on the web backend.
 */
class PronunciationPipeline(context: Context) {

    companion object {
        private const val TAG = "PronunciationPipeline"
    }

    private val recognizer = PhonemeRecognizer(context)
    private val g2p = G2p(context)

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
        expectedPhones: List<String>,
        displayText: String,
        sampleRate: Int = 16000
    ): PronunciationResult {
        if (!isReady) {
            Log.w(TAG, "Model not ready")
            return PronunciationResult(
                score = 0f,
                expectedIpa = expectedPhones.joinToString(" "),
                heardIpa = "",
                wordErrors = emptyList(),
                phonemeAccuracy = 0f,
                wordAccuracy = 0f,
                acousticDistance = 0f,
                feedback = "Model not loaded. Please restart the app."
            )
        }

        // 1. Letter-level CTC decode -> clean text (like transcribe + clean_transcription).
        //    fuzzy=true: ASR near-misses ("warter") snap to the closest real word.
        val recognition = recognizer.transcribePhones(audioData, sampleRate)
        val heardText = cleanTranscription(recognition.phones)
        val heard = PhoneNormalizer.normalizeSequence(g2p.textToPhones(heardText, fuzzy = true))

        // 2. Text -> espeak IPA phones for BOTH sides, so alignment is apples-to-apples,
        //    keeping word boundaries for the per-word feedback.
        var runningIndex = 0
        val targets = g2p.textToWordPhones(displayText).map { (text, phones) ->
            val normalized = PhoneNormalizer.normalizeSequence(phones)
            val tw = com.pronouncecoach.android.scoring.TargetWord(text, normalized, runningIndex)
            runningIndex += normalized.size
            tw
        }
        val expected = targets.flatMap { it.phones }

        Log.i(TAG, "Heard text: \"$heardText\" | Expected: $expected | Heard: $heard")

        // 3. Score with the near-phone weighted scheme of the web backend.
        return PronunciationScorer.score(expected, heard, targets)
    }

    /** Lower-case letters/apostrophes only; '|' word markers become spaces. */
    private fun cleanTranscription(tokens: List<String>): String {
        val raw = tokens.joinToString("").replace("|", " ").lowercase()
        return raw.replace(Regex("[^a-z' ]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun close() {
        recognizer.close()
    }
}
