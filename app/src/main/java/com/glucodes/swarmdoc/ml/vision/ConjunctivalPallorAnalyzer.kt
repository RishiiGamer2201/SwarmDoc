package com.glucodes.swarmdoc.ml.vision

import android.graphics.Bitmap
import android.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes the conjunctiva (inner lower eyelid) for pallor to screen for anemia.
 * Implements a heuristic based on published medical literature:
 * converting RGB to LAB color space and analyzing the a* (red/green) channel.
 *
 * Normal conjunctiva is rich in blood vessels (high a* value).
 * Pallor (anemia) presents as pale/white (low a* value, higher L* value).
 */
@Singleton
class ConjunctivalPallorAnalyzer @Inject constructor() {

    data class Result(
        val severity: Severity,
        val estimatedHbRange: String,
        val meanAStar: Float,
        val meanLStar: Float,
        val confidence: Float
    ) {
        enum class Severity(val label: String) {
            NORMAL("Normal"),
            MILD_PALLOR("Mild Pallor"),
            MODERATE_PALLOR("Moderate Pallor"),
            SEVERE_PALLOR("Severe Pallor")
        }
    }

    /**
     * Analyze an extracted bitmap of the conjunctival region.
     * The bitmap should preferably be cropped to just the inner eyelid tissue.
     */
    fun analyze(bitmap: Bitmap): Result {
        val width = bitmap.width
        val height = bitmap.height

        var sumAStar = 0.0
        var sumLStar = 0.0
        var validPixels = 0

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Basic filtering to exclude pure white/black boundaries or glaring reflections
            if ((r > 240 && g > 240 && b > 240) || (r < 20 && g < 20 && b < 20)) {
                continue
            }

            val lab = rgbToLab(r, g, b)
            sumLStar += lab[0]
            sumAStar += lab[1]
            validPixels++
        }

        if (validPixels == 0) {
            return Result(Result.Severity.NORMAL, "Invalid", 0f, 0f, 0f)
        }

        val meanAStar = (sumAStar / validPixels).toFloat()
        val meanLStar = (sumLStar / validPixels).toFloat()

        // Inference thresholds (Heuristics based on general LAB a* variance in conjunctiva)
        // a* ranges roughly from 0 (neutral/grey) to 100 (pure red/magenta)
        // Highly vascular conjunctiva usually scores higher on the a* axis.
        val severity = when {
            meanAStar > 25f -> Result.Severity.NORMAL
            meanAStar > 20f -> Result.Severity.MILD_PALLOR
            meanAStar > 14f -> Result.Severity.MODERATE_PALLOR
            else -> Result.Severity.SEVERE_PALLOR
        }

        val estimatedHbRange = when (severity) {
            Result.Severity.NORMAL -> "> 11.0 g/dL"
            Result.Severity.MILD_PALLOR -> "9.0 - 11.0 g/dL"
            Result.Severity.MODERATE_PALLOR -> "7.0 - 9.0 g/dL"
            Result.Severity.SEVERE_PALLOR -> "< 7.0 g/dL"
        }

        // Confidence heuristic (higher if brightness L* is reasonable, 40-70 range)
        var confidence = 1.0f - (Math.abs(meanLStar - 55f) / 55f)
        confidence = confidence.coerceIn(0.4f, 0.95f)

        return Result(severity, estimatedHbRange, meanAStar, meanLStar, confidence)
    }

    /**
     * Converts RGB [0..255] to CIELAB color space.
     * Returns double array: [L*, a*, b*]
     */
    private fun rgbToLab(R: Int, G: Int, B: Int): DoubleArray {
        // 1. Convert RGB to XYZ
        var r = R / 255.0
        var g = G / 255.0
        var b = B / 255.0

        r = if (r > 0.04045) Math.pow((r + 0.055) / 1.055, 2.4) else r / 12.92
        g = if (g > 0.04045) Math.pow((g + 0.055) / 1.055, 2.4) else g / 12.92
        b = if (b > 0.04045) Math.pow((b + 0.055) / 1.055, 2.4) else b / 12.92

        r *= 100.0
        g *= 100.0
        b *= 100.0

        // Observer. = 2°, Illuminant = D65
        val x = r * 0.4124 + g * 0.3576 + b * 0.1805
        val y = r * 0.2126 + g * 0.7152 + b * 0.0722
        val z = r * 0.0193 + g * 0.1192 + b * 0.9505

        // 2. Convert XYZ to LAB
        var xRef = x / 95.047
        var yRef = y / 100.000
        var zRef = z / 108.883

        xRef = if (xRef > 0.008856) Math.pow(xRef, 1.0 / 3.0) else (7.787 * xRef) + (16.0 / 116.0)
        yRef = if (yRef > 0.008856) Math.pow(yRef, 1.0 / 3.0) else (7.787 * yRef) + (16.0 / 116.0)
        zRef = if (zRef > 0.008856) Math.pow(zRef, 1.0 / 3.0) else (7.787 * zRef) + (16.0 / 116.0)

        val L = (116.0 * yRef) - 16.0
        val a = 500.0 * (xRef - yRef)
        val bVal = 200.0 * (yRef - zRef)

        return doubleArrayOf(L, a, bVal)
    }
}
