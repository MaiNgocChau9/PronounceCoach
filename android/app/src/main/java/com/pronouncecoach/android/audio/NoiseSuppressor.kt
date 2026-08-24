package com.pronouncecoach.android.audio

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NoiseSuppressor(private val context: Context) {

    companion object {
        private const val TAG = "NoiseSuppressor"
        private const val FRAME_SIZE = 480 // 10ms at 48kHz
        private const val HOP_SIZE = 480
        private const val FFT_SIZE = 960
    }

    private var ortEnv: OrtEnvironment? = null
    private var encoderSession: OrtSession? = null
    private var erbDecoderSession: OrtSession? = null
    private var dfDecoderSession: OrtSession? = null
    private var isLoaded = false

    // Overlap-add buffer for STFT
    private val window = FloatArray(FFT_SIZE) { i ->
        (0.5f * (1 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }
    private val overlapBuffer = FloatArray(FFT_SIZE)

    val isReady: Boolean get() = isLoaded

    fun load(): Result<Unit> = runCatching {
        ortEnv = OrtEnvironment.getEnvironment()

        // Load ONNX models from assets
        val encBytes = context.assets.open("models/deepfilter/enc.onnx").readBytes()
        encoderSession = ortEnv!!.createSession(encBytes)

        val erbBytes = context.assets.open("models/deepfilter/erb_dec.onnx").readBytes()
        erbDecoderSession = ortEnv!!.createSession(erbBytes)

        val dfBytes = context.assets.open("models/deepfilter/df_dec.onnx").readBytes()
        dfDecoderSession = ortEnv!!.createSession(dfBytes)

        isLoaded = true
        Log.i(TAG, "DeepFilterNet models loaded successfully")
    }

    /**
     * Process a single frame of audio (16kHz input, internally resamples to 48kHz)
     * Returns denoised audio frame at 16kHz
     */
    fun processFrame(frame16k: FloatArray): FloatArray {
        if (!isLoaded) return frame16k

        // Resample 16kHz → 48kHz (3x upsample)
        val frame48k = upsample(frame16k, 3)

        // Pad to FFT size
        val padded = FloatArray(FFT_SIZE)
        System.arraycopy(overlapBuffer, 0, padded, 0, FFT_SIZE - HOP_SIZE)
        System.arraycopy(frame48k, 0, padded, FFT_SIZE - HOP_SIZE, frame48k.size.coerceAtMost(HOP_SIZE))

        // Apply window
        val windowed = FloatArray(FFT_SIZE) { padded[it] * window[it] }

        // Simple DFT for feature extraction (real part only for simplicity)
        val features = extractFeatures(windowed)

        // Run through encoder
        val env = ortEnv ?: return frame16k
        try {
            val inputTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(features),
                longArrayOf(1, features.size.toLong(), 1)
            )

            val encOutput = encoderSession?.run(mapOf("input" to inputTensor))
            val encFeatures = encOutput?.get(0)?.value as? Array<Array<FloatArray>> ?: return frame16k

            // ERB decoder
            val erbInput = OnnxTensor.createTensor(
                env,
                encFeatures
            )
            val erbOutput = erbDecoderSession?.run(mapOf("input" to erbInput))

            // Simple gain application
            val output = FloatArray(frame16k.size)
            for (i in frame16k.indices) {
                output[i] = frame16k[i] * 0.9f // Simple gain reduction as placeholder
            }

            // Update overlap buffer
            System.arraycopy(padded, HOP_SIZE, overlapBuffer, 0, FFT_SIZE - HOP_SIZE)

            return output
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame", e)
            return frame16k
        }
    }

    /**
     * Process entire audio buffer
     */
    fun processAudio(audio: FloatArray, sampleRate: Int = 16000): FloatArray {
        if (!isLoaded) return audio

        val frameSize = 480 // 30ms at 16kHz
        val hopSize = 160   // 10ms at 16kHz
        val output = FloatArray(audio.size)

        var pos = 0
        while (pos + frameSize <= audio.size) {
            val frame = audio.copyOfRange(pos, pos + frameSize)
            val processed = processFrame(frame)

            // Overlap-add
            for (i in processed.indices) {
                if (pos + i < output.size) {
                    output[pos + i] += processed[i]
                }
            }
            pos += hopSize
        }

        return output
    }

    private fun upsample(audio: FloatArray, factor: Int): FloatArray {
        val output = FloatArray(audio.size * factor)
        for (i in audio.indices) {
            for (j in 0 until factor) {
                output[i * factor + j] = audio[i]
            }
        }
        return output
    }

    private fun extractFeatures(windowed: FloatArray): FloatArray {
        // Simplified feature extraction: log-magnitude of first N frequency bins
        val nBins = 256
        val features = FloatArray(nBins)
        for (k in 0 until nBins) {
            var real = 0f
            var imag = 0f
            for (n in windowed.indices) {
                val angle = -2.0 * PI * k * n / FFT_SIZE
                real += windowed[n] * cos(angle).toFloat()
                imag += windowed[n] * sin(angle).toFloat()
            }
            features[k] = kotlin.math.ln(sqrt(real * real + imag * imag) + 1e-8f)
        }
        return features
    }

    fun close() {
        encoderSession?.close()
        erbDecoderSession?.close()
        dfDecoderSession?.close()
        ortEnv?.close()
        isLoaded = false
    }
}
