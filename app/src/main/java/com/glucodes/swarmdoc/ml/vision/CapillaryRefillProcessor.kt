package com.glucodes.swarmdoc.ml.vision

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Processes fingernail video frames to estimate Capillary Refill Time (CRT).
 * Algorithm:
 * 1. Analyze the red channel intensity in the Region of Interest (ROI) over time.
 * 2. Detect blanching (nail turns white/pale, red intensity drops).
 * 3. Detect release (red intensity starts increasing).
 * 4. Measure time taken for red intensity to return to baseline.
 */
@Singleton
class CapillaryRefillProcessor @Inject constructor() {

    enum class State {
        IDLE,           // Waiting for blanching
        BLANCHED,       // Nail is pressed (white)
        REFILLING,      // Pressure released, tracking color return
        COMPLETE        // Refill complete
    }

    data class Result(
        val refillTimeMs: Long,
        val status: String,
        val confidence: Float
    )

    private var currentState = State.IDLE
    private var baselineRed = 0f
    private var blanchedRed = 0f
    private val frameHistory = mutableListOf<FrameData>()
    
    private var blanchStartTime = 0L
    private var releaseTime = 0L
    private var refillCompleteTime = 0L

    // Thresholds
    private val BLANCH_DROP_THRESHOLD = 0.15f // 15% drop in red intensity
    private val REFILL_COMPLETE_THRESHOLD = 0.90f // 90% return to baseline

    data class FrameData(
        val timestamp: Long,
        val normalizedRed: Float
    )

    /**
     * Feed a frame (bitmap of the fingernail ROI) and its timestamp.
     * Returns the current processing state and an optional result if complete.
     */
    fun processFrame(bitmap: Bitmap, timestamp: Long): Pair<State, Result?> {
        val redIntensity = extractRedIntensity(bitmap)
        
        // Keep last ~3 seconds of history (assuming 30fps = ~90 frames)
        if (frameHistory.size > 90) {
            frameHistory.removeAt(0)
        }
        frameHistory.add(FrameData(timestamp, redIntensity))

        when (currentState) {
            State.IDLE -> {
                // Determine baseline if not set (average of first 10 frames)
                if (baselineRed == 0f && frameHistory.size >= 10) {
                    baselineRed = frameHistory.take(10).map { it.normalizedRed }.average().toFloat()
                }

                // Look for blanching (significant drop from baseline)
                if (baselineRed > 0 && redIntensity < baselineRed * (1 - BLANCH_DROP_THRESHOLD)) {
                    currentState = State.BLANCHED
                    blanchStartTime = timestamp
                    blanchedRed = redIntensity
                }
            }
            State.BLANCHED -> {
                // Update lowest blanched value
                if (redIntensity < blanchedRed) {
                    blanchedRed = redIntensity
                }
                
                // Detect release (sudden increase from blanched state)
                // Require pressure to be held for at least 1 second
                if (timestamp - blanchStartTime > 1000 && redIntensity > blanchedRed * 1.1f) {
                    currentState = State.REFILLING
                    releaseTime = timestamp
                }
            }
            State.REFILLING -> {
                // Check if red intensity has returned to near baseline
                val targetRed = blanchedRed + (baselineRed - blanchedRed) * REFILL_COMPLETE_THRESHOLD
                if (redIntensity >= targetRed) {
                    currentState = State.COMPLETE
                    refillCompleteTime = timestamp
                }
            }
            State.COMPLETE -> {
                // Already complete, ignore further frames until reset
            }
        }

        var result: Result? = null
        if (currentState == State.COMPLETE) {
            val crtMs = refillCompleteTime - releaseTime
            val status = when {
                crtMs < 2000 -> "Normal"
                crtMs < 4000 -> "Slow (Possible Dehydration/Shock)"
                else -> "Delayed (Critical Issue)"
            }
            // Simple confidence metric based on the depth of the blanching
            val depthRatio = (baselineRed - blanchedRed) / baselineRed
            val confidence = (depthRatio * 2f).coerceIn(0f, 1f)
            
            result = Result(crtMs, status, confidence)
        }

        return Pair(currentState, result)
    }

    fun reset() {
        currentState = State.IDLE
        baselineRed = 0f
        blanchedRed = 0f
        frameHistory.clear()
        blanchStartTime = 0L
        releaseTime = 0L
        refillCompleteTime = 0L
    }

    /**
     * Extracts the average normalized red intensity from the bitmap.
     * Normalized against total intensity to reduce lighting variations.
     */
    private fun extractRedIntensity(bitmap: Bitmap): Float {
        var sumRed = 0L
        var sumTotal = 0L
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample every Nth pixel for speed (e.g., a 10x10 grid)
        val stepX = maxOf(1, width / 20)
        val stepY = maxOf(1, height / 20)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = pixels[y * width + x]
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                sumRed += r
                sumTotal += (r + g + b).coerceAtLeast(1)
            }
        }

        // Return ratio of red to total light (avoids absolute brightness issues)
        return if (sumTotal > 0) sumRed.toFloat() / sumTotal.toFloat() else 0f
    }
}
