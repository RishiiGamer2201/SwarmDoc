package com.glucodes.swarmdoc.ml.audio

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Clinical Decision API wrapper for ONNX cough classification.
 * Uses the custom MLP trained on acoustic features, functioning 100% offline.
 */
@Singleton
class CoughClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CoughClassifier"
        private val MODEL_NAME_CANDIDATES = listOf(
            "cough_classifier_v1.onnx",
            "cough_classifier.onnx",
            "cough classifier.onnx"
        )
        private const val NUM_FEATURES = 16 // 13 MFCC + Spectral Centroid + Rolloff + ZCR
        private const val TARGET_SECONDS = 3
        private const val N_FFT = 512
        private const val HOP_LENGTH = 256
        private const val N_MELS = 26
        private const val N_MFCC = 13
        private const val ROLLOFF_PERCENT = 0.85f
        
        // TODO: Replace with exact constants printed by train_cough_mlp.py
        val FEATURE_MEAN = FloatArray(NUM_FEATURES) { 0f }
        val FEATURE_STD = FloatArray(NUM_FEATURES) { 1f }
    }

    data class CoughResult(
        val pattern: String,
        val patternLocal: String,
        val confidence: Float,
        val recommendation: String,
        val isTBSuspected: Boolean = false,
    )

    private var isModelLoaded = false
    private var ortEnvironment: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var lastModelError: String? = null

    init {
        initialize()
    }

    fun initialize() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()

            val modelName = resolveModelAssetName()
                ?: throw IllegalStateException("No cough ONNX model found in assets")
            // 100% offline loading with fallback: try direct bytes first, then cache path.
            val modelBytes = context.assets.open(modelName).readBytes()
            val sessionOptions = OrtSession.SessionOptions()
            session = try {
                ortEnvironment?.createSession(modelBytes, sessionOptions)
            } catch (directLoadError: Throwable) {
                Log.w(TAG, "Direct model-byte load failed, retrying via cache file: ${directLoadError.message}")
                val modelFile = File(context.cacheDir, modelName)
                FileOutputStream(modelFile).use { it.write(modelBytes) }
                // For external-data ONNX exports, copy the sidecar data file next to the model.
                copyCompanionDataAssetIfPresent(modelName)
                ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
            }

            if (session == null) {
                throw IllegalStateException("ORT session returned null")
            }
            isModelLoaded = true
            lastModelError = null
            Log.d(TAG, "ONNX Cough Classifier loaded successfully for offline inference ($modelName).")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to load ONNX model: ${e.message}", e)
            isModelLoaded = false
            lastModelError = buildErrorMessage(e)
        }
    }

    /**
     * Classify a cough audio sample completely offline.
     * @param audioData PCM audio data (16kHz, 16-bit, mono)
     * @param sampleRate Audio sample rate (default 16000)
     */
    suspend fun classify(audioData: ShortArray, sampleRate: Int = 16000): CoughResult = withContext(Dispatchers.Default) {
        if (!isModelLoaded || session == null || ortEnvironment == null) {
            Log.w(TAG, "Model not available for inference.")
            return@withContext CoughResult(
                pattern = "Model Unavailable",
                patternLocal = "मॉडल उपलब्ध नहीं",
                confidence = 0.0f,
                recommendation = "Cough model is not loaded. ${lastModelError ?: "Please reinstall the app build with ONNX runtime support."}",
                isTBSuspected = false,
            )
        }

        try {
            val quality = analyzeSignalQuality(audioData)
            if (!quality.isUsable) {
                return@withContext CoughResult(
                    pattern = "Low Signal",
                    patternLocal = "कम ऑडियो सिग्नल",
                    confidence = 0f,
                    recommendation = "Audio signal too weak/noisy for cough analysis. Record again in a quieter place and keep phone 10-15 cm from mouth.",
                    isTBSuspected = false
                )
            }
            val coughPresence = analyzeCoughPresence(audioData, sampleRate)
            if (!coughPresence.isLikelyCough) {
                return@withContext CoughResult(
                    pattern = "No Cough Detected",
                    patternLocal = "खांसी नहीं मिली",
                    confidence = coughPresence.confidence,
                    recommendation = "Audio does not contain a clear cough burst. Please record while the patient coughs 2-3 times.",
                    isTBSuspected = false
                )
            }

            // 1. Extract acoustic features (Offline processing)
            val features = extractFeatures(audioData, sampleRate)
            
            // 2. Normalize features
            val useFallbackNorm = FEATURE_MEAN.all { it == 0f } && FEATURE_STD.all { it == 1f }
            if (useFallbackNorm) {
                val localMean = features.average().toFloat()
                var localVar = 0f
                for (f in features) localVar += (f - localMean) * (f - localMean)
                val localStd = sqrt(localVar / features.size).coerceAtLeast(1e-6f)
                for (i in features.indices) {
                    features[i] = (features[i] - localMean) / localStd
                }
            } else {
                for (i in features.indices) {
                    features[i] = (features[i] - FEATURE_MEAN[i]) / (FEATURE_STD[i] + 1e-8f)
                }
            }

            // 3. Prepare ONNX Input Tensor
            val inputShape = longArrayOf(1, NUM_FEATURES.toLong())
            val fb = FloatBuffer.wrap(features)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, fb, inputShape)
            
            val inputName = session!!.inputNames.iterator().next()
            val inputs = mapOf(inputName to inputTensor)

            // 4. Run offline inference
            val results = session!!.run(inputs)
            val outputValue = results[0].value
            val classProbs = when (outputValue) {
                is Array<*> -> {
                    val first = outputValue.firstOrNull()
                    if (first is FloatArray) first else FloatArray(0)
                }
                is FloatArray -> outputValue
                else -> FloatArray(0)
            }
            
            results.close()
            inputTensor.close()

            if (classProbs.isEmpty()) {
                return@withContext CoughResult(
                    pattern = "Model Output Error",
                    patternLocal = "आउटपुट त्रुटि",
                    confidence = 0f,
                    recommendation = "Model returned an unexpected output shape.",
                    isTBSuspected = false
                )
            }

            // 5. Post-process to map outputs (0=healthy, 1=wet/bronchitis, 2=covid)
            var maxIdx = 0
            var maxProb = classProbs[0]
            for (i in 1 until classProbs.size) {
                if (classProbs[i] > maxProb) {
                    maxProb = classProbs[i]
                    maxIdx = i
                }
            }

            return@withContext when (maxIdx) {
                0 -> CoughResult(
                    pattern = "Healthy/Dry Cough",
                    patternLocal = "सामान्य सूखी खांसी",
                    confidence = maxProb,
                    recommendation = "No severe pathology detected. Ensure hydration.",
                    isTBSuspected = false
                )
                1 -> CoughResult(
                    pattern = "Wet Cough / Bronchial",
                    patternLocal = "बलगम वाली खांसी",
                    confidence = maxProb,
                    recommendation = "Signs of congestion. If persistent for >2 weeks, evaluate for tuberculosis.",
                    isTBSuspected = true 
                )
                else -> CoughResult(
                    pattern = "Atypical Cough (COVID Pattern)",
                    patternLocal = "कोविड जैसी खांसी",
                    confidence = maxProb,
                    recommendation = "Acoustic signatures match specific viral loads. Monitor vitals closely.",
                    isTBSuspected = false
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Offline inference error: ${e.message}", e)
            return@withContext CoughResult(
                pattern = "Analysis Error",
                patternLocal = "त्रुटि",
                confidence = 0f,
                recommendation = "Model inference failed: ${buildErrorMessage(e)}",
                isTBSuspected = false
            )
        }
    }

    /**
     * Extract 16 features aligned with training script:
     * 13 MFCC means + spectral centroid mean + rolloff mean + ZCR mean.
     */
    private fun extractFeatures(audioData: ShortArray, sampleRate: Int): FloatArray {
        val features = FloatArray(NUM_FEATURES) { 0f }
        if (audioData.size < N_FFT) return features

        val targetSamples = TARGET_SECONDS * sampleRate
        val clipped = if (audioData.size >= targetSamples) {
            audioData.copyOfRange(0, targetSamples)
        } else {
            ShortArray(targetSamples).also { out ->
                System.arraycopy(audioData, 0, out, 0, audioData.size)
            }
        }
        val x = FloatArray(clipped.size) { i -> clipped[i] / 32768f }
        val window = hammingWindow(N_FFT)
        val melBank = melFilterBank(sampleRate, N_FFT, N_MELS)
        val mfccAcc = FloatArray(N_MFCC) { 0f }
        var frameCount = 0
        var centroidAcc = 0f
        var rolloffAcc = 0f
        var zcrAcc = 0f
        var pos = 0
        while (pos + N_FFT <= x.size) {
            val frame = FloatArray(N_FFT)
            for (i in 0 until N_FFT) frame[i] = x[pos + i] * window[i]
            val power = powerSpectrum(frame)

            val melEnergies = FloatArray(N_MELS)
            for (m in 0 until N_MELS) {
                var e = 0f
                for (k in melBank[m].indices) {
                    e += melBank[m][k] * power[k]
                }
                melEnergies[m] = ln(e + 1e-8f)
            }
            val mfcc = dct(melEnergies, N_MFCC)
            for (i in 0 until N_MFCC) mfccAcc[i] += mfcc[i]

            var magSum = 0f
            var weightedFreq = 0f
            for (k in power.indices) {
                val freq = k * sampleRate.toFloat() / N_FFT
                val mag = sqrt(power[k])
                magSum += mag
                weightedFreq += freq * mag
            }
            centroidAcc += if (magSum > 0f) weightedFreq / magSum else 0f

            val totalPower = power.sum()
            val targetPower = totalPower * ROLLOFF_PERCENT
            var cumsum = 0f
            var rolloffBin = 0
            for (k in power.indices) {
                cumsum += power[k]
                if (cumsum >= targetPower) {
                    rolloffBin = k
                    break
                }
            }
            rolloffAcc += rolloffBin * sampleRate.toFloat() / N_FFT

            var frameZcr = 0
            for (i in 1 until N_FFT) {
                if ((frame[i] >= 0 && frame[i - 1] < 0) || (frame[i] < 0 && frame[i - 1] >= 0)) frameZcr++
            }
            zcrAcc += frameZcr.toFloat() / N_FFT

            frameCount++
            pos += HOP_LENGTH
        }

        if (frameCount == 0) return features
        for (i in 0 until N_MFCC) features[i] = mfccAcc[i] / frameCount
        features[13] = centroidAcc / frameCount
        features[14] = rolloffAcc / frameCount
        features[15] = zcrAcc / frameCount
        return features
    }

    private data class SignalQuality(
        val isUsable: Boolean,
    )

    private data class CoughPresence(
        val isLikelyCough: Boolean,
        val confidence: Float
    )

    private fun analyzeSignalQuality(audioData: ShortArray): SignalQuality {
        if (audioData.size < 2000) return SignalQuality(false)
        var sumSq = 0.0
        var nonZero = 0
        for (s in audioData) {
            val v = s.toFloat() / 32768f
            sumSq += (v * v).toDouble()
            if (abs(v) > 0.01f) nonZero++
        }
        val rms = sqrt(sumSq / audioData.size).toFloat()
        val activeRatio = nonZero.toFloat() / audioData.size.toFloat()
        return SignalQuality(
            isUsable = rms > 0.01f && activeRatio > 0.05f
        )
    }

    private fun analyzeCoughPresence(audioData: ShortArray, sampleRate: Int): CoughPresence {
        val frameSize = max(128, sampleRate / 40) // ~25ms
        if (audioData.size < frameSize * 4) return CoughPresence(false, 0f)

        val totalFrames = audioData.size / frameSize
        val frameRms = FloatArray(totalFrames)
        var globalRmsAcc = 0f
        for (f in 0 until totalFrames) {
            val start = f * frameSize
            val end = min(audioData.size, start + frameSize)
            var sumSq = 0f
            for (i in start until end) {
                val v = audioData[i] / 32768f
                sumSq += v * v
            }
            val rms = sqrt(sumSq / max(1f, (end - start).toFloat()))
            frameRms[f] = rms
            globalRmsAcc += rms
        }
        val meanRms = globalRmsAcc / max(1, totalFrames).toFloat()
        val burstThreshold = max(0.03f, meanRms * 2.2f)
        val coughLikeFrames = frameRms.count { it > burstThreshold }
        val coughLikeRatio = coughLikeFrames.toFloat() / totalFrames.toFloat()

        // Cough tends to be short impulsive bursts, not mostly continuous speech/noise.
        val likely = coughLikeFrames >= 2 && coughLikeRatio in 0.02f..0.35f
        val confidence = (coughLikeFrames / 6f).coerceIn(0f, 1f)
        return CoughPresence(likely, confidence)
    }

    private fun hammingWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.54 - 0.46 * cos((2.0 * Math.PI * i) / (size - 1))).toFloat()
        }
    }

    private fun powerSpectrum(frame: FloatArray): FloatArray {
        val bins = N_FFT / 2 + 1
        val out = FloatArray(bins)
        for (k in 0 until bins) {
            var real = 0.0
            var imag = 0.0
            for (n in frame.indices) {
                val angle = 2.0 * Math.PI * k * n / N_FFT
                val v = frame[n].toDouble()
                real += v * cos(angle)
                imag -= v * sin(angle)
            }
            out[k] = ((real * real + imag * imag) / N_FFT).toFloat()
        }
        return out
    }

    private fun hzToMel(hz: Float): Float = (2595.0 * ln(1.0 + hz / 700.0)).toFloat()
    private fun melToHz(mel: Float): Float = (700.0 * (Math.exp((mel / 2595.0).toDouble()) - 1.0)).toFloat()

    private fun melFilterBank(sampleRate: Int, nFft: Int, nMels: Int): Array<FloatArray> {
        val bins = nFft / 2 + 1
        val fMin = 0f
        val fMax = sampleRate / 2f
        val melMin = hzToMel(fMin)
        val melMax = hzToMel(fMax)
        val melPoints = FloatArray(nMels + 2) { i ->
            melMin + (melMax - melMin) * i / (nMels + 1)
        }
        val hzPoints = FloatArray(nMels + 2) { i -> melToHz(melPoints[i]) }
        val bin = IntArray(nMels + 2) { i ->
            ((nFft + 1) * hzPoints[i] / sampleRate).toInt().coerceIn(0, bins - 1)
        }

        val fb = Array(nMels) { FloatArray(bins) }
        for (m in 1..nMels) {
            val left = bin[m - 1]
            val center = bin[m]
            val right = bin[m + 1]
            if (center > left) {
                for (k in left until center) {
                    fb[m - 1][k] = (k - left).toFloat() / (center - left).toFloat()
                }
            }
            if (right > center) {
                for (k in center until right) {
                    fb[m - 1][k] = (right - k).toFloat() / (right - center).toFloat()
                }
            }
        }
        return fb
    }

    private fun dct(input: FloatArray, outSize: Int): FloatArray {
        val n = input.size
        val out = FloatArray(outSize)
        for (k in 0 until outSize) {
            var sum = 0.0
            for (i in 0 until n) {
                sum += input[i] * cos(Math.PI * k * (i + 0.5) / n)
            }
            out[k] = sum.toFloat()
        }
        return out
    }

    fun release() {
        session?.close()
        // Do not close OrtEnvironment here because it's process-global and may be shared.
        isModelLoaded = false
    }

    fun isReady(): Boolean = isModelLoaded && session != null && ortEnvironment != null

    fun getModelStatusMessage(): String {
        return if (isReady()) "Model ready" else (lastModelError ?: "Model not initialized")
    }

    private fun buildErrorMessage(error: Throwable): String {
        val type = error::class.java.simpleName
        val message = error.message?.takeIf { it.isNotBlank() } ?: "Unknown error"
        return "$type: $message"
    }

    private fun resolveModelAssetName(): String? {
        val assets = context.assets.list("")?.toSet().orEmpty()
        return MODEL_NAME_CANDIDATES.firstOrNull { it in assets }
            ?: assets.firstOrNull { it.endsWith(".onnx", ignoreCase = true) && it.contains("cough", ignoreCase = true) }
    }

    private fun copyCompanionDataAssetIfPresent(modelName: String) {
        val candidateDataNames = listOf(
            "$modelName.data",
            modelName.replace(".onnx", ".onnx.data", ignoreCase = true)
        ).distinct()
        for (dataAssetName in candidateDataNames) {
            try {
                context.assets.open(dataAssetName).use { input ->
                    FileOutputStream(File(context.cacheDir, dataAssetName)).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Copied companion ONNX data asset: $dataAssetName")
                return
            } catch (_: Exception) {
                // Asset variant not present; keep trying other naming patterns.
            }
        }
    }
}
