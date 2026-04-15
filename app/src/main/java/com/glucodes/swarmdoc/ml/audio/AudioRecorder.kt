package com.glucodes.swarmdoc.ml.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Real microphone recording using Android AudioRecord API.
 * Records PCM16 at 16kHz mono and provides:
 * - FloatArray buffer for ML inference (normalized to [-1, 1])
 * - Real-time amplitude level for waveform visualization
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val AUDIO_SOURCE_CANDIDATES = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.CAMCORDER
        )
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    init {
        _isReady.value = hasPermission()
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Record audio for [durationMs] milliseconds.
     * Returns the PCM float array normalized to [-1, 1] range.
     */
    suspend fun recordAudio(durationMs: Long): FloatArray = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return@withContext FloatArray(0)
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid min buffer size: $minBufferSize")
            return@withContext FloatArray(0)
        }
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        try {
            audioRecord = createAudioRecord(bufferSize)

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return@withContext FloatArray(0)
            }

            val totalSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
            val allSamples = ShortArray(totalSamples)
            var samplesRead = 0

            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "Recording started: ${durationMs}ms, expecting $totalSamples samples")

            val readBuffer = ShortArray(1024)
            val startTime = System.currentTimeMillis()

            while (isRecording && samplesRead < totalSamples && isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= durationMs + 500) break // Safety timeout

                val toRead = minOf(readBuffer.size, totalSamples - samplesRead)
                val result = audioRecord?.read(readBuffer, 0, toRead) ?: -1

                if (result > 0) {
                    System.arraycopy(readBuffer, 0, allSamples, samplesRead, result)
                    samplesRead += result

                    // Update live amplitude (RMS of current chunk)
                    var sumSquares = 0.0
                    for (i in 0 until result) {
                        val sample = readBuffer[i].toFloat() / Short.MAX_VALUE
                        sumSquares += sample * sample
                    }
                    _amplitude.value = sqrt(sumSquares / result).toFloat().coerceIn(0f, 1f)
                } else if (result == AudioRecord.ERROR_DEAD_OBJECT) {
                    Log.e(TAG, "AudioRecord dead object while reading")
                    break
                } else if (result == AudioRecord.ERROR_INVALID_OPERATION || result == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Audio read error code: $result")
                }
            }

            stopRecording()
            Log.d(TAG, "Recording complete: $samplesRead samples captured")

            // Convert ShortArray to normalized FloatArray
            return@withContext shortArrayToFloat(allSamples, samplesRead)

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            stopRecording()
            return@withContext FloatArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Recording error: ${e.message}")
            stopRecording()
            return@withContext FloatArray(0)
        }
    }

    /**
     * Record continuously and call [onChunk] with each audio chunk.
     * Used for streaming analysis (e.g., cough detection in real-time).
     */
    suspend fun recordStreaming(
        chunkDurationMs: Long = 975, // ~1 YAMNet frame
        onChunk: suspend (FloatArray) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid min buffer size: $minBufferSize")
            return@withContext
        }
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        try {
            audioRecord = createAudioRecord(bufferSize)

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) return@withContext

            val chunkSamples = (SAMPLE_RATE * chunkDurationMs / 1000).toInt()
            val chunkBuffer = ShortArray(chunkSamples)

            audioRecord?.startRecording()
            isRecording = true

            while (isRecording && isActive) {
                var samplesRead = 0
                while (samplesRead < chunkSamples && isRecording) {
                    val result = audioRecord?.read(
                        chunkBuffer, samplesRead, chunkSamples - samplesRead
                    ) ?: -1
                    if (result > 0) {
                        samplesRead += result
                    } else if (result == AudioRecord.ERROR_DEAD_OBJECT) {
                        Log.e(TAG, "Streaming read failed: dead object")
                        break
                    } else if (result == AudioRecord.ERROR_INVALID_OPERATION || result == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "Streaming read error code: $result")
                        break
                    } else {
                        break
                    }
                }

                if (samplesRead > 0) {
                    val floatChunk = shortArrayToFloat(chunkBuffer, samplesRead)
                    // Update amplitude
                    var sumSq = 0.0
                    for (i in 0 until minOf(samplesRead, 512)) {
                        val s = chunkBuffer[i].toFloat() / Short.MAX_VALUE
                        sumSq += s * s
                    }
                    _amplitude.value = sqrt(sumSq / minOf(samplesRead, 512)).toFloat().coerceIn(0f, 1f)

                    onChunk(floatChunk)
                }
            }

            stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Streaming recording error: ${e.message}")
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _amplitude.value = 0f
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    /**
     * Record audio and return raw ShortArray (PCM16) for ONNX CoughClassifier.
     */
    suspend fun recordAudioRaw(durationMs: Long): ShortArray = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return@withContext ShortArray(0)
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            Log.e(TAG, "Invalid min buffer size: $minBufferSize")
            return@withContext ShortArray(0)
        }
        val bufferSize = minBufferSize.coerceAtLeast(4096)

        try {
            audioRecord = createAudioRecord(bufferSize)

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return@withContext ShortArray(0)
            }

            val totalSamples = (SAMPLE_RATE * durationMs / 1000).toInt()
            val allSamples = ShortArray(totalSamples)
            var samplesRead = 0

            audioRecord?.startRecording()
            isRecording = true

            val readBuffer = ShortArray(1024)
            val startTime = System.currentTimeMillis()

            while (isRecording && samplesRead < totalSamples && isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= durationMs + 500) break

                val toRead = minOf(readBuffer.size, totalSamples - samplesRead)
                val result = audioRecord?.read(readBuffer, 0, toRead) ?: -1

                if (result > 0) {
                    System.arraycopy(readBuffer, 0, allSamples, samplesRead, result)
                    samplesRead += result

                    var sumSquares = 0.0
                    for (i in 0 until result) {
                        val sample = readBuffer[i].toFloat() / Short.MAX_VALUE
                        sumSquares += sample * sample
                    }
                    _amplitude.value = sqrt(sumSquares / result).toFloat().coerceIn(0f, 1f)
                } else if (result == AudioRecord.ERROR_DEAD_OBJECT) {
                    Log.e(TAG, "Raw read failed: dead object")
                    break
                } else if (result == AudioRecord.ERROR_INVALID_OPERATION || result == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Raw read error code: $result")
                }
            }

            stopRecording()
            Log.d(TAG, "Raw recording complete: $samplesRead samples captured")

            return@withContext if (samplesRead < totalSamples) allSamples.copyOf(samplesRead) else allSamples

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            stopRecording()
            return@withContext ShortArray(0)
        } catch (e: Exception) {
            Log.e(TAG, "Recording error: ${e.message}")
            stopRecording()
            return@withContext ShortArray(0)
        }
    }

    private fun shortArrayToFloat(shorts: ShortArray, count: Int): FloatArray {
        val floats = FloatArray(count)
        for (i in 0 until count) {
            floats[i] = shorts[i].toFloat() / Short.MAX_VALUE.toFloat()
        }
        return floats
    }

    private fun createAudioRecord(bufferSize: Int): AudioRecord? {
        for (source in AUDIO_SOURCE_CANDIDATES) {
            try {
                val candidate = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    Log.d(TAG, "AudioRecord initialized with source=$source, buffer=$bufferSize")
                    return candidate
                }
                candidate.release()
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord init failed for source=$source: ${e.message}")
            }
        }
        return null
    }
}
