package com.pronouncecoach.android.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.pronouncecoach.android.scoring.CtcDecoder
import java.nio.FloatBuffer
import kotlin.math.sqrt

class PhonemeRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "PhonemeRecognizer"
        private const val SAMPLE_RATE = 16000
        private const val MAX_SECONDS = 20
    }

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var inputName: String = ""
    private var vocab: List<String> = emptyList()
    private var isLoaded = false

    val isReady: Boolean get() = isLoaded

    fun load(modelPath: String, vocabPath: String): Result<Unit> = runCatching {
        Log.i(TAG, "Loading model from assets: $modelPath")
        ortEnv = OrtEnvironment.getEnvironment()

        val modelBytes = context.assets.open(modelPath).readBytes()
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(4, 8)

        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(threads)
            setInterOpNumThreads(1)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        session = ortEnv!!.createSession(modelBytes, opts)
        inputName = session!!.inputNames.first()
        Log.i(TAG, "ONNX session ready (intra-op threads: $threads)")

        vocab = context.assets.open(vocabPath).bufferedReader().readLines()
            .filter { it.isNotEmpty() }
        Log.i(TAG, "Vocab size: ${vocab.size}")

        isLoaded = true
        warmUp()
    }

    /** One throwaway inference so the first real analysis skips lazy kernel setup. */
    private fun warmUp() {
        runCatching {
            val silence = FloatArray(SAMPLE_RATE / 2) // 0.5 s of zeros
            infer(silence)
            Log.i(TAG, "Warm-up inference done")
        }.onFailure { Log.w(TAG, "Warm-up failed (harmless): ${it.message}") }
    }

    /**
     * Recognizes phones in a waveform: trims leading/trailing silence (proportional
     * speed-up, fewer junk phones), runs the ONNX session and decodes CTC greedily
     * with per-phone confidences.
     */
    fun transcribePhones(audioData: FloatArray, sampleRate: Int = SAMPLE_RATE): CtcDecoder.PhoneResult {
        check(isLoaded) { "Model not loaded" }

        val resampled = if (sampleRate != SAMPLE_RATE) resample(audioData, sampleRate, SAMPLE_RATE) else audioData
        val trimmed = SilenceTrimmer.trim(resampled, SAMPLE_RATE)

        return infer(trimmed)
    }

    private fun infer(audio: FloatArray): CtcDecoder.PhoneResult {
        val env = ortEnv!!
        val sess = session!!

        // Wav2Vec2 expects raw samples normalized per utterance.
        normalize(audio)
        val clipped = if (audio.size > SAMPLE_RATE * MAX_SECONDS) audio.copyOf(SAMPLE_RATE * MAX_SECONDS) else audio

        OnnxTensor.createTensor(env, FloatBuffer.wrap(clipped), longArrayOf(1, clipped.size.toLong())).use { input ->
            sess.run(mapOf(inputName to input)).use { results ->
                val output = results[0] as OnnxTensor
                val shape = output.info.shape // [1, frames, vocab]
                val frames = shape[1].toInt()
                val vocabSize = shape[2].toInt()
                val buffer: FloatBuffer = output.floatBuffer
                Log.d(TAG, "Logits [1, $frames, $vocabSize]")
                return CtcDecoder.decode(buffer, frames, vocabSize, vocab)
            }
        }
    }

    /** Zero-mean unit-variance normalization over the whole clip, like HF's processor. */
    private fun normalize(audio: FloatArray) {
        var mean = 0.0
        for (v in audio) mean += v
        mean /= audio.size.coerceAtLeast(1)
        var variance = 0.0
        for (v in audio) variance += (v - mean) * (v - mean)
        val std = sqrt(variance / audio.size.coerceAtLeast(1)).toFloat().coerceAtLeast(1e-7f)
        for (i in audio.indices) audio[i] = ((audio[i] - mean.toFloat()) / std)
    }

    private fun resample(audio: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return audio
        val ratio = fromRate.toFloat() / toRate
        val newLength = (audio.size / ratio).toInt()
        return FloatArray(newLength) { i ->
            val srcPos = i * ratio
            val srcIdx = srcPos.toInt()
            val frac = srcPos - srcIdx
            when {
                srcIdx + 1 < audio.size -> audio[srcIdx] * (1 - frac) + audio[srcIdx + 1] * frac
                srcIdx < audio.size -> audio[srcIdx]
                else -> 0f
            }
        }
    }

    fun close() {
        session?.close()
        session = null
        isLoaded = false
    }
}
