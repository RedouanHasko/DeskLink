package com.example.network

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioStreamer(
    private val scope: CoroutineScope,
    private val onAudioChunk: (String) -> Unit
) {
    private val TAG = "DeskLinkAudioStreamer"
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun startStreaming() {
        if (_isRecording.value) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return
            }

            audioRecord?.startRecording()
            _isRecording.value = true

            recordJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(1024)
                val byteBuffer = ByteArray(2048)

                while (isActive && _isRecording.value) {
                    val readShorts = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readShorts > 0) {
                        // Calculate RMS amplitude
                        var sum = 0.0
                        for (i in 0 until readShorts) {
                            val sample = buffer[i].toDouble()
                            sum += sample * sample
                            // convert short to byteBuffer (little endian)
                            byteBuffer[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
                        }
                        val rms = sqrt(sum / readShorts)
                        val norm = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        _amplitude.value = norm

                        // Send base64 audio chunk
                        val base64Chunk = Base64.encodeToString(byteBuffer, 0, readShorts * 2, Base64.NO_WRAP)
                        onAudioChunk(base64Chunk)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio streamer", e)
            stopStreaming()
        }
    }

    fun stopStreaming() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _amplitude.value = 0f
    }
}
