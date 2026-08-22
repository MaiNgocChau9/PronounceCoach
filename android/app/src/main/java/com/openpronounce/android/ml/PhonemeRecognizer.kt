package com.openpronounce.android.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer

class PhonemeRecognizer(private val context: Context) {

    companion object {
        private const val TAG = "PhonemeRecognizer"
    }

    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var vocab: List<String> = emptyList()
    private var isLoaded = false

    val isReady: Boolean get() = isLoaded

    fun load(modelPath: String, vocabPath: String): Result<Unit> = runCatching {
        Log.i(TAG, "Loading model from assets: $modelPath")
        ortEnv = OrtEnvironment.getEnvironment()

        val modelBytes = context.assets.open(modelPath).readBytes()
        Log.i(TAG, "Model bytes loaded: ${modelBytes.size}")

        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
        }
        session = ortEnv!!.createSession(modelBytes, opts)

        vocab = context.assets.open(vocabPath).bufferedReader().readLines()
            .filter { it.isNotEmpty() }
        Log.i(TAG, "Vocab size: ${vocab.size}")

        isLoaded = true
        Log.i(TAG, "Model loaded successfully!")
    }

    fun transcribe(audioData: FloatArray, sampleRate: Int = 16000): String {
        if (!isLoaded) {
            Log.e(TAG, "Model not loaded!")
            return ""
        }

        val env = ortEnv!!
        val sess = session!!

        // Resample if needed
        val resampled = if (sampleRate != 16000) {
            resample(audioData, sampleRate, 16000)
        } else {
            audioData
        }

        Log.d(TAG, "Audio length: ${resampled.size} samples (${resampled.size / 16000.0}s)")

        // Pad or trim to reasonable length (max 30s)
        val maxLen = 16000 * 30
        val audio = if (resampled.size > maxLen) {
            resampled.copyOf(maxLen)
        } else {
            resampled
        }

        // Create input tensor [1, audio_length]
        val inputTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(audio),
            longArrayOf(1, audio.size.toLong())
        )

        // Run inference
        val inputName = sess.inputNames.first()
        Log.d(TAG, "Running inference, input: $inputName, shape: [1, ${audio.size}]")

        val results = sess.run(mapOf(inputName to inputTensor))

        // Get logits [1, frames, vocab_size]
        @Suppress("UNCHECKED_CAST")
        val output = results[0].value as Array<Array<FloatArray>>
        val logits = output[0] // [frames, vocab_size]

        Log.d(TAG, "Output shape: [${logits.size}, ${logits[0].size}]")

        // Greedy CTC decode
        val text = ctcDecode(logits)

        inputTensor.close()
        results.close()

        return text
    }

    private fun ctcDecode(logits: Array<FloatArray>): String {
        val sb = StringBuilder()
        var lastTokenId = -1

        for (frame in logits.indices) {
            // Find argmax
            var maxId = 0
            var maxVal = logits[frame][0]
            for (j in 1 until logits[frame].size) {
                if (logits[frame][j] > maxVal) {
                    maxVal = logits[frame][j]
                    maxId = j
                }
            }

            // CTC: skip blank (id=0) and repeated tokens
            if (maxId != 0 && maxId != lastTokenId) {
                val token = vocab.getOrElse(maxId) { "<unk>" }
                if (token.isNotEmpty() && token != "<s>" && token != "</s>" && token != "<pad>" && token != "|") {
                    sb.append(token)
                } else if (token == "|") {
                    sb.append(" ") // word boundary
                }
            }
            lastTokenId = maxId
        }

        return sb.toString().trim()
    }

    private fun resample(audio: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return audio
        val ratio = fromRate.toFloat() / toRate
        val newLength = (audio.size / ratio).toInt()
        return FloatArray(newLength) { i ->
            val srcPos = i * ratio
            val srcIdx = srcPos.toInt()
            val frac = srcPos - srcIdx
            if (srcIdx + 1 < audio.size) {
                audio[srcIdx] * (1 - frac) + audio[srcIdx + 1] * frac
            } else if (srcIdx < audio.size) {
                audio[srcIdx]
            } else {
                0f
            }
        }
    }

    fun close() {
        session?.close()
        ortEnv?.close()
        isLoaded = false
    }
}
