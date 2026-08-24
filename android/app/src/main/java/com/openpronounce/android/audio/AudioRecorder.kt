package com.openpronounce.android.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Chunked PCM storage: one ShortArray per read, no per-sample boxing.
    private val chunks = ArrayList<ShortArray>()
    @Volatile
    private var totalSamples = 0

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    private val _recordedData = MutableStateFlow<FloatArray?>(null)
    val recordedData: StateFlow<FloatArray?> = _recordedData

    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording() {
        if (!hasPermission()) {
            Log.e(TAG, "Microphone permission not granted")
            return
        }
        stopRecording()

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )
        // NOTE: deliberately NO NoiseSuppressor / AcousticEchoCanceler here.
        // VOICE_RECOGNITION already requests a clean, unprocessed path; attaching NS/AEC
        // distorts vowels enough that the CTC model mishears them (e.g. /ɪ/ -> /k/).
        // DeepFilterNet-style denoise would run in software if ever needed.

        synchronized(chunks) {
            chunks.clear()
            totalSamples = 0
        }
        _recordedData.value = null
        _audioLevel.value = 0f
        audioRecord = record
        record.startRecording()
        _isRecording.value = true

        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive && _isRecording.value) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    chunks.add(buffer.copyOf(read))
                    totalSamples += read

                    var sum = 0.0
                    for (i in 0 until read) {
                        val s = buffer[i].toFloat()
                        sum += s * s
                    }
                    val rms = (sqrt(sum / read) / Short.MAX_VALUE).toFloat()
                    _audioLevel.value = rms.coerceIn(0f, 1f)
                }
            }
            // The loop exits through the flag set by stopRecording(), so this always runs.
            _recordedData.value = toFloatArray()
        }
    }

    private fun toFloatArray(): FloatArray? {
        if (totalSamples == 0) return null
        val data = FloatArray(totalSamples)
        var offset = 0
        val scale = 1f / Short.MAX_VALUE
        synchronized(chunks) {
            for (chunk in chunks) {
                for (s in chunk) data[offset++] = s * scale
            }
            chunks.clear()
            totalSamples = 0
        }
        return data
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
        audioRecord?.release()
        audioRecord = null
    }

    /** Waits until the reader loop has flushed the recorded PCM, then returns it. */
    suspend fun awaitRecording(): FloatArray? {
        recordingJob?.join()
        return _recordedData.value
    }

    fun getRecordedAudio(): FloatArray? = _recordedData.value

    fun clearRecording() {
        synchronized(chunks) { chunks.clear(); totalSamples = 0 }
        _recordedData.value = null
        _audioLevel.value = 0f
    }

    fun destroy() {
        stopRecording()
        scope.cancel()
    }
}
