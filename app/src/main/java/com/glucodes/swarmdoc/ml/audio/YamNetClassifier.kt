package com.glucodes.swarmdoc.ml.audio

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio classifier using YAMNet TFLite model for cough detection.
 *
 * YAMNet classifies 521 audio event classes. We map relevant classes
 * to cough categories:
 * - Class 36: "Cough"
 * - Class 37: "Throat clearing"
 * - Class 30: "Breathing"
 * - Class 31: "Wheeze"
 *
 * Model file: assets/model/yamnet.tflite (~13MB)
 * Download URL: https://tfhub.dev/google/lite-model/yamnet/tflite/1
 */
@Singleton
class YamNetClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "YamNetClassifier"
        private const val MODEL_PATH = "model/yamnet.tflite"
        private const val SAMPLE_RATE = 16000
        private const val FRAME_LENGTH = 15600 // ~0.975 seconds at 16kHz
        // YAMNet class indices for cough-related sounds
        private const val CLASS_COUGH = 36
        private const val CLASS_THROAT_CLEARING = 37
        private const val CLASS_BREATHING = 30
        private const val CLASS_WHEEZE = 31
        private const val CLASS_SNEEZE = 38
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelFile = loadModelFile()
            if (modelFile != null) {
                interpreter = Interpreter(modelFile)
                _isReady.value = true
                Log.d(TAG, "YAMNet model loaded successfully")
            } else {
                Log.w(TAG, "YAMNet model file not found at $MODEL_PATH. Place yamnet.tflite in app/src/main/assets/model/")
                _isReady.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load YAMNet model: ${e.message}")
            _isReady.value = false
        }
    }

    private fun loadModelFile(): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(MODEL_PATH)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            null
        }
    }

    data class CoughResult(
        val isCoughDetected: Boolean,
        val coughType: String, // "dry_cough", "wet_cough", "no_cough"
        val confidence: Float,
        val allScores: Map<String, Float> = emptyMap(),
    )

    /**
     * Classify audio buffer for cough detection.
     * @param audioBuffer PCM float array at 16kHz
     */
    suspend fun classify(audioBuffer: FloatArray): CoughResult = withContext(Dispatchers.Default) {
        val start = System.nanoTime()

        if (interpreter == null || !_isReady.value) {
            Log.w(TAG, "Model not ready, returning rule-based fallback")
            return@withContext fallbackClassify(audioBuffer)
        }

        try {
            // Prepare input: YAMNet expects [1, FRAME_LENGTH] float waveform
            val inputSize = FRAME_LENGTH.coerceAtMost(audioBuffer.size)
            val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4).apply {
                order(ByteOrder.nativeOrder())
                for (i in 0 until inputSize) {
                    putFloat(audioBuffer[i])
                }
                rewind()
            }

            // Output: [1, 521] class scores
            val outputArray = Array(1) { FloatArray(521) }
            interpreter?.run(inputBuffer, outputArray)

            val scores = outputArray[0]
            val coughScore = scores.getOrElse(CLASS_COUGH) { 0f }
            val throatScore = scores.getOrElse(CLASS_THROAT_CLEARING) { 0f }
            val wheezeScore = scores.getOrElse(CLASS_WHEEZE) { 0f }
            val breathingScore = scores.getOrElse(CLASS_BREATHING) { 0f }

            val combinedCoughScore = coughScore + throatScore * 0.5f
            val isCough = combinedCoughScore > 0.3f

            val coughType = when {
                !isCough -> "no_cough"
                wheezeScore > 0.2f -> "wet_cough"
                else -> "dry_cough"
            }

            val elapsed = (System.nanoTime() - start) / 1_000_000
            Log.d(TAG, "Classification done in ${elapsed}ms: type=$coughType, conf=$combinedCoughScore")

            CoughResult(
                isCoughDetected = isCough,
                coughType = coughType,
                confidence = combinedCoughScore.coerceIn(0f, 1f),
                allScores = mapOf(
                    "cough" to coughScore,
                    "throat_clearing" to throatScore,
                    "wheeze" to wheezeScore,
                    "breathing" to breathingScore,
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error: ${e.message}")
            return@withContext fallbackClassify(audioBuffer)
        }
    }

    /**
     * Rule-based fallback when model is not available.
     * Uses basic signal energy analysis.
     */
    private fun fallbackClassify(audioBuffer: FloatArray): CoughResult {
        // Simple energy-based heuristic
        val energy = audioBuffer.map { it * it }.average()
        val peak = audioBuffer.maxOrNull() ?: 0f

        val isCough = energy > 0.01 && peak > 0.3f

        return CoughResult(
            isCoughDetected = isCough,
            coughType = if (isCough) "dry_cough" else "no_cough",
            confidence = if (isCough) 0.6f else 0.2f,
            allScores = mapOf("energy_based" to energy.toFloat()),
        )
    }

    fun release() {
        interpreter?.close()
        interpreter = null
        _isReady.value = false
    }
}
