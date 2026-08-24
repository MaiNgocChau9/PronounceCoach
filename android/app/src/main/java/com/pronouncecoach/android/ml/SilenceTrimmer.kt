package com.pronouncecoach.android.ml

import kotlin.math.sqrt

/**
 * Drops leading/trailing silence, keeping a safety margin around the detected speech so
 * soft onsets and fading tails are never cut. Mirrors the Python pipeline's validated
 * approach: shorter input means proportionally faster inference and fewer junk phones
 * decoded from silence.
 */
object SilenceTrimmer {

    fun trim(
        audio: FloatArray,
        sampleRate: Int = 16000,
        topDb: Float = 45f,
        marginMs: Int = 300
    ): FloatArray {
        val frameLen = sampleRate / 25      // 40 ms window
        val hop = frameLen / 2              // 20 ms step
        if (audio.size < frameLen * 2) return audio

        val nFrames = (audio.size - frameLen) / hop + 1
        val rms = FloatArray(nFrames)
        for (f in 0 until nFrames) {
            var sum = 0.0
            val start = f * hop
            for (i in start until start + frameLen) {
                val v = audio[i].toDouble()
                sum += v * v
            }
            rms[f] = sqrt(sum / frameLen).toFloat()
        }

        var peak = 0f
        for (v in rms) if (v > peak) peak = v
        if (peak < 1e-4f) return audio      // near-digital silence: keep everything

        val threshold = peak * Math.pow(10.0, (-topDb / 20.0)).toFloat()
        var first = -1
        var last = -1
        for (f in 0 until nFrames) {
            if (rms[f] > threshold) {
                if (first < 0) first = f
                last = f
            }
        }
        if (first < 0) return audio

        val margin = marginMs * sampleRate / 1000
        val startSample = maxOf(0, first * hop - margin)
        val endSample = minOf(audio.size, (last * hop + frameLen) + margin)
        if (endSample - startSample < sampleRate / 4 || endSample - startSample < audio.size / 10) {
            return audio                    // suspiciously short result: keep the original
        }
        return audio.copyOfRange(startSample, endSample)
    }
}
