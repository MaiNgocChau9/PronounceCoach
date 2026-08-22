package com.openpronounce.android.scoring

object CtcDecoder {

    data class PhoneResult(
        val phones: List<String>,
        val confidences: List<Float>,
        val spans: List<Pair<Int, Int>>
    )

    fun decode(
        logits: FloatArray,
        vocab: List<String>,
        numFrames: Int,
        vocabSize: Int
    ): PhoneResult {
        val blankId = 0
        val phones = mutableListOf<String>()
        val confidences = mutableListOf<Float>()
        val spans = mutableListOf<Pair<Int, Int>>()

        var lastToken = -1
        var frameStart = 0

        for (frame in 0 until numFrames) {
            val offset = frame * vocabSize
            var maxId = 0
            var maxVal = logits[offset]
            var sumExp = 0f

            // Softmax per frame for confidence
            for (k in 0 until vocabSize) {
                val v = logits[offset + k]
                if (v > maxVal) {
                    maxVal = v
                    maxId = k
                }
            }

            // Compute softmax for the winning token
            for (k in 0 until vocabSize) {
                sumExp += Math.exp((logits[offset + k] - maxVal).toDouble()).toFloat()
            }
            val confidence = Math.exp(0.0).toFloat() / sumExp // softmax of max

            if (maxId != blankId && maxId != lastToken) {
                val phone = vocab.getOrElse(maxId) { "<unk>" }
                if (phone.isNotEmpty() && phone != "<s>" && phone != "</s>" && phone != "<pad>") {
                    phones.add(phone)
                    confidences.add(coerceIn(confidence, 0f, 1f))
                    spans.add(Pair(frameStart, frame))
                }
                frameStart = frame
            }
            lastToken = maxId
        }

        return PhoneResult(phones, confidences, spans)
    }

    private fun coerceIn(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}
