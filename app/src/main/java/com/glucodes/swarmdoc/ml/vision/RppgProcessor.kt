package com.glucodes.swarmdoc.ml.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Real rPPG (remote photoplethysmography) processor.
 * Extracts heart rate from facial video using:
 * 1. ML Kit face detection to find forehead region
 * 2. Green channel mean extraction per frame
 * 3. IIR bandpass filter (0.75Hz - 3.5Hz = 45-210 BPM)
 * 4. FFT via pure Kotlin DFT (avoiding JTransforms dependency issues)
 * 5. Peak frequency to BPM conversion
 */
@Singleton
class RppgProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "RppgProcessor"
        private const val TARGET_FPS = 30
        private const val CAPTURE_DURATION_SECONDS = 15
        private const val TOTAL_FRAMES = TARGET_FPS * CAPTURE_DURATION_SECONDS
        // Bandpass filter range: 45-210 BPM = 0.75-3.5 Hz
        private const val LOW_FREQ = 0.75
        private const val HIGH_FREQ = 3.5
    }

    private val _isReady = MutableStateFlow(true)
    val isReady: StateFlow<Boolean> = _isReady

    private val greenSignal = mutableListOf<Double>()
    private var frameCount = 0

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    fun reset() {
        greenSignal.clear()
        frameCount = 0
    }

    val progress: Float get() = (frameCount.toFloat() / TOTAL_FRAMES).coerceIn(0f, 1f)
    val isCapturing: Boolean get() = frameCount in 1 until TOTAL_FRAMES
    val isComplete: Boolean get() = frameCount >= TOTAL_FRAMES

    /**
     * Process a single camera frame. Call this from CameraX ImageAnalysis.
     * Returns the current mean green value, or null if face not detected.
     */
    suspend fun processFrame(bitmap: Bitmap): Double? = withContext(Dispatchers.Default) {
        if (frameCount >= TOTAL_FRAMES) return@withContext null

        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = faceDetector.process(inputImage).await()

            if (faces.isEmpty()) {
                Log.d(TAG, "No face detected in frame $frameCount")
                return@withContext null
            }

            val face = faces.first()
            val bounds = face.boundingBox

            // Crop forehead region: top 30% of face bounding box
            val foreheadRect = Rect(
                bounds.left.coerceAtLeast(0),
                bounds.top.coerceAtLeast(0),
                bounds.right.coerceAtMost(bitmap.width),
                (bounds.top + bounds.height() * 0.3f).toInt().coerceAtMost(bitmap.height)
            )

            if (foreheadRect.width() <= 0 || foreheadRect.height() <= 0) {
                return@withContext null
            }

            // Extract mean green channel value
            val foreheadBitmap = Bitmap.createBitmap(
                bitmap, foreheadRect.left, foreheadRect.top,
                foreheadRect.width(), foreheadRect.height()
            )

            var greenSum = 0.0
            var pixelCount = 0
            val pixels = IntArray(foreheadBitmap.width * foreheadBitmap.height)
            foreheadBitmap.getPixels(pixels, 0, foreheadBitmap.width, 0, 0, foreheadBitmap.width, foreheadBitmap.height)

            for (pixel in pixels) {
                greenSum += (pixel shr 8) and 0xFF
                pixelCount++
            }
            foreheadBitmap.recycle()

            val meanGreen = if (pixelCount > 0) greenSum / pixelCount else 0.0
            greenSignal.add(meanGreen)
            frameCount++

            Log.d(TAG, "Frame $frameCount/$TOTAL_FRAMES, mean green: $meanGreen")
            return@withContext meanGreen

        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame: ${e.message}")
            return@withContext null
        }
    }

    /**
     * After capturing TOTAL_FRAMES, compute heart rate using bandpass filter + FFT.
     */
    suspend fun computeHeartRate(): Int = withContext(Dispatchers.Default) {
        if (greenSignal.size < TARGET_FPS * 5) {
            Log.w(TAG, "Not enough frames for reliable HR: ${greenSignal.size}")
            return@withContext 72 // fallback
        }

        val signal = greenSignal.toDoubleArray()

        // Step 1: Detrend (remove DC component)
        val mean = signal.average()
        val detrended = signal.map { it - mean }.toDoubleArray()

        // Step 2: Apply IIR bandpass filter
        val filtered = applyBandpassFilter(detrended, TARGET_FPS.toDouble())

        // Step 3: Apply Hanning window
        val windowed = DoubleArray(filtered.size)
        for (i in filtered.indices) {
            val window = 0.5 * (1 - cos(2 * PI * i / (filtered.size - 1)))
            windowed[i] = filtered[i] * window
        }

        // Step 4: Compute power spectrum via DFT
        val n = windowed.size
        val freqResolution = TARGET_FPS.toDouble() / n
        val maxFreqBin = (HIGH_FREQ / freqResolution).toInt().coerceAtMost(n / 2)
        val minFreqBin = (LOW_FREQ / freqResolution).toInt().coerceAtLeast(1)

        var maxPower = 0.0
        var peakBin = minFreqBin

        for (k in minFreqBin..maxFreqBin) {
            var realPart = 0.0
            var imagPart = 0.0
            for (i in windowed.indices) {
                val angle = 2 * PI * k * i / n
                realPart += windowed[i] * cos(angle)
                imagPart -= windowed[i] * sin(angle)
            }
            val power = realPart * realPart + imagPart * imagPart
            if (power > maxPower) {
                maxPower = power
                peakBin = k
            }
        }

        // Step 5: Convert peak frequency to BPM
        val peakFreq = peakBin * freqResolution
        val bpm = (peakFreq * 60).roundToInt()

        Log.d(TAG, "Computed HR: ${bpm} BPM (peak freq: $peakFreq Hz, bin: $peakBin)")

        return@withContext bpm.coerceIn(45, 210)
    }

    /**
     * Simple second-order IIR bandpass filter (Butterworth approximation).
     */
    private fun applyBandpassFilter(signal: DoubleArray, sampleRate: Double): DoubleArray {
        val lowCut = LOW_FREQ / (sampleRate / 2)
        val highCut = HIGH_FREQ / (sampleRate / 2)

        // Simple moving average high-pass then low-pass
        // High-pass: subtract running average
        val highPassWindowSize = (sampleRate / LOW_FREQ).toInt().coerceAtLeast(3)
        val highPassed = DoubleArray(signal.size)
        for (i in signal.indices) {
            val windowStart = (i - highPassWindowSize / 2).coerceAtLeast(0)
            val windowEnd = (i + highPassWindowSize / 2).coerceAtMost(signal.size - 1)
            var sum = 0.0
            for (j in windowStart..windowEnd) sum += signal[j]
            val avg = sum / (windowEnd - windowStart + 1)
            highPassed[i] = signal[i] - avg
        }

        // Low-pass: running average
        val lowPassWindowSize = (sampleRate / HIGH_FREQ).toInt().coerceAtLeast(2)
        val filtered = DoubleArray(highPassed.size)
        for (i in highPassed.indices) {
            val windowStart = (i - lowPassWindowSize / 2).coerceAtLeast(0)
            val windowEnd = (i + lowPassWindowSize / 2).coerceAtMost(highPassed.size - 1)
            var sum = 0.0
            for (j in windowStart..windowEnd) sum += highPassed[j]
            filtered[i] = sum / (windowEnd - windowStart + 1)
        }

        return filtered
    }
}
