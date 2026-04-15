package com.glucodes.swarmdoc.ml.llm

/**
 * JNI interface for gemma-2b-it via llama.cpp.
 * This class provides the complete JNI method signatures for LLM inference.
 * The actual .so library must be placed in jniLibs/ for real inference.
 * Falls back to RegexExtractor when native library is unavailable.
 */
class GemmaInterface {

    companion object {
        private var isLibraryLoaded = false

        init {
            try {
                System.loadLibrary("llama")
                isLibraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                isLibraryLoaded = false
            }
        }

        fun isAvailable(): Boolean = isLibraryLoaded
    }

    // JNI method signatures (implemented in native C++ layer)
    private external fun nativeInit(modelPath: String, contextSize: Int, threads: Int): Long
    private external fun nativeInfer(handle: Long, prompt: String, maxTokens: Int, temperature: Float): String
    private external fun nativeTokenize(handle: Long, text: String): IntArray
    private external fun nativeRelease(handle: Long)

    private var modelHandle: Long = 0

    /**
     * Initialize the LLM with a GGUF model file.
     * @param modelPath Absolute path to the .gguf model file
     * @param contextSize Context window size (default 2048 for gemma-2b)
     * @param threads Number of CPU threads for inference
     */
    fun initialize(modelPath: String, contextSize: Int = 2048, threads: Int = 4): Boolean {
        if (!isLibraryLoaded) return false
        return try {
            modelHandle = nativeInit(modelPath, contextSize, threads)
            modelHandle != 0L
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Run inference with a structured extraction prompt.
     * @param patientNotes Raw transcribed notes from ASHA worker
     * @return Structured extraction as JSON string
     */
    fun extractStructuredData(patientNotes: String): String? {
        if (!isLibraryLoaded || modelHandle == 0L) return null

        val prompt = """
            |<start_of_turn>user
            |Extract structured medical information from these patient notes. 
            |Return JSON with fields: chief_complaint, duration, vitals, medications, red_flags.
            |
            |Patient notes: $patientNotes
            |<end_of_turn>
            |<start_of_turn>model
        """.trimMargin()

        return try {
            nativeInfer(modelHandle, prompt, 256, 0.1f)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tokenize text for analysis.
     */
    fun tokenize(text: String): IntArray? {
        if (!isLibraryLoaded || modelHandle == 0L) return null
        return try {
            nativeTokenize(modelHandle, text)
        } catch (e: Exception) {
            null
        }
    }

    fun release() {
        if (isLibraryLoaded && modelHandle != 0L) {
            try {
                nativeRelease(modelHandle)
            } catch (_: Exception) {}
            modelHandle = 0
        }
    }
}
