package com.openpronounce.android.scoring

import java.nio.FloatBuffer

/**
 * Greedy CTC decoding with per-phone confidence and frame spans, mirroring the Python
 * pipeline's decode_ctc: repeated frames are collapsed, blanks and special tokens
 * dropped, each phone gets the softmax posterior of its token as confidence and the
 * half-open frame range it was decoded from.
 */
object CtcDecoder {

    data class PhoneResult(
        val phones: List<String>,
        val confidences: List<Float>,
        /** Half-open frame ranges [start, end) of each phone. */
        val spans: List<IntRange>
    )

    fun decode(
        logits: FloatBuffer,
        numFrames: Int,
        vocabSize: Int,
        vocab: List<String>,
        blankId: Int = 0
    ): PhoneResult {
        val frame = FloatArray(vocabSize)
        val phones = mutableListOf<String>()
        val confidences = mutableListOf<Float>()
        val spans = mutableListOf<IntRange>()
        var openStart = -1     // start frame of the span currently being extended, -1 = none
        var lastId = -1

        for (f in 0 until numFrames) {
            for (k in 0 until vocabSize) frame[k] = logits.get(f * vocabSize + k)

            var id = 0
            var maxVal = frame[0]
            for (k in 1 until vocabSize) {
                if (frame[k] > maxVal) {
                    maxVal = frame[k]
                    id = k
                }
            }

            if (id != lastId) {
                if (openStart >= 0) {                       // close the previous phone
                    spans[spans.size - 1] = openStart until f
                    openStart = -1
                }
                if (id != blankId && isRealToken(vocab.getOrElse(id) { "" })) {
                    phones.add(vocab[id])
                    confidences.add(softmax(frame, id))
                    spans.add(IntRange(f, f))               // provisional end, closed below
                    openStart = f
                }
            }
            lastId = id
        }
        if (openStart >= 0) spans[spans.size - 1] = openStart until numFrames

        return PhoneResult(phones, confidences, spans)
    }

    private fun softmax(frame: FloatArray, token: Int): Float {
        var sum = 0.0
        for (v in frame) sum += Math.exp((v - frame[token]).toDouble())
        return (1.0 / sum).toFloat().coerceIn(0f, 1f)
    }

    /** Special vocabulary entries are wrapped in angle brackets ("<pad>", "<s>", ...). */
    fun isRealToken(token: String): Boolean {
        return token.isNotEmpty() && !token.startsWith("<") && !token.endsWith(">")
    }
}
