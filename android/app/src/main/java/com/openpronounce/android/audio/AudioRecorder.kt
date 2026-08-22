package com.openpronounce.android.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    private val _recordedData = MutableStateFlow<FloatArray?>(null)
    val recordedData: StateFlow<FloatArray?> = _recordedData

    private var pcmBuffer = mutableListOf<Short>()

    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): Result<Unit> = runCatching {
        if (!hasPermission()) {
            throw SecurityException("Microphone permission not granted")
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            .coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        pcmBuffer.clear()
        _isRecording.value = true

        audioRecord?.startRecording()

        recordingJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive && _isRecording.value) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    // Store PCM data
                    val shorts = buffer.copyOf(read)
                    pcmBuffer.addAll(shorts.toTypedArray())

                    // Compute audio level for visualization
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += shorts[i].toDouble() * shorts[i].toDouble()
                    }
                    val rms = Math.sqrt(sum / read) / Short.MAX_VALUE
                    _audioLevel.value = rms.toFloat().coerceIn(0f, 1f)
                }
            }

            // Convert PCM to float array
            val floatData = FloatArray(pcmBuffer.size)
            for (i in pcmBuffer.indices) {
                floatData[i] = pcmBuffer[i].toFloat() / Short.MAX_VALUE
            }
            _recordedData.value = floatData
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordingJob?.cancel()
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
        audioRecord?.release()
        audioRecord = null
    }

    fun getRecordedAudio(): FloatArray? {
        return _recordedData.value
    }

    fun clearRecording() {
        pcmBuffer.clear()
        _recordedData.value = null
        _audioLevel.value = 0f
    }

    fun destroy() {
        stopRecording()
        scope.cancel()
    }
}
