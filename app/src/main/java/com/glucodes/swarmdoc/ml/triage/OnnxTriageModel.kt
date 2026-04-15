package com.glucodes.swarmdoc.ml.triage

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lightweight Clinical Decision Model using ONNX Runtime.
 * Runs a small PyTorch-trained MLP for multi-class disease classification and triage.
 */
@Singleton
class OnnxTriageModel @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var ortEnvironment: OrtEnvironment? = null
    private var session: OrtSession? = null

    // Assumed Input/Output specifications based on simple MLP
    // Input shape: [1, NumFeatures]
    // numFeatures = age(1) + sex(2) + temperature(1) + symptom_flags(N)
    companion object {
        const val MODEL_NAME = "triage_mlp.onnx"
        // Let's assume the model is trained on our 30 base symptoms + 4 vitals/demographics
        const val EXPECTED_FEATURES = 34 
    }

    init {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            loadModel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModel() {
        try {
            // Check if model file exists in assets, copy to cache if so
            val modelBytes = context.assets.open(MODEL_NAME).readBytes()
            val modelFile = File(context.cacheDir, MODEL_NAME)
            FileOutputStream(modelFile).use { it.write(modelBytes) }
            
            val sessionOptions = OrtSession.SessionOptions()
            session = ortEnvironment?.createSession(modelFile.absolutePath, sessionOptions)
        } catch (e: Exception) {
            // Expected if model file is not present yet (stub phase)
        }
    }

    /**
     * Run inference on the provided feature vector.
     * @param features FloatArray of size EXPECTED_FEATURES
     * @return FloatArray of class probabilities, or null if model not loaded
     */
    suspend fun infer(features: FloatArray): FloatArray? = withContext(Dispatchers.Default) {
        val ortSession = session ?: return@withContext null
        val env = ortEnvironment ?: return@withContext null

        if (features.size != EXPECTED_FEATURES) {
            return@withContext null
        }

        try {
            // Prepare inputs
            val inputShape = longArrayOf(1, EXPECTED_FEATURES.toLong())
            val fb = FloatBuffer.wrap(features)
            val inputTensor = OnnxTensor.createTensor(env, fb, inputShape)
            
            val inputName = ortSession.inputNames.iterator().next()
            val inputs = mapOf(inputName to inputTensor)

            // Run inference
            val results = ortSession.run(inputs)
            
            // Get output (assumes a single output tensor with shape [1, NumClasses])
            val output = results[0].value as Array<FloatArray>
            val classProbs = output[0]
            
            results.close()
            inputTensor.close()
            
            return@withContext classProbs
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    fun close() {
        session?.close()
        ortEnvironment?.close()
    }
}
