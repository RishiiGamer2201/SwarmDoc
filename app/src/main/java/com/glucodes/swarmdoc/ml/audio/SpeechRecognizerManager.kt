package com.glucodes.swarmdoc.ml.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's SpeechRecognizer for real-time speech-to-text.
 * Supports:
 * - Streaming partial results
 * - Hindi + English bilingual recognition
 * - Continuous listening mode (auto-restart on silence)
 * - Offline recognition on Android 11+ (if downloaded)
 */
@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SpeechRecognizerMgr"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    init {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start listening and return a Flow of transcription updates.
     * Each emission is a Pair<String, Boolean> where:
     * - first: the transcribed text so far
     * - second: whether this is a final result (vs partial)
     *
     * @param language BCP-47 language code ("en-IN", "hi-IN")
     * @param continuous If true, auto-restarts on silence for continuous dictation
     */
    fun startListening(
        language: String = "en-IN",
        continuous: Boolean = true
    ): Flow<Pair<String, Boolean>> = callbackFlow {
        if (!_isAvailable.value) {
            Log.e(TAG, "Speech recognition not available")
            close()
            return@callbackFlow
        }

        var accumulatedText = ""
        var shouldRestart = continuous

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Try to use offline recognition
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Extended silence lengths for medical dictation
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Could expose this for waveform visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }
                Log.w(TAG, "Recognition error: $errorMsg")

                // On no-match or timeout, restart if continuous
                if (shouldRestart && (error == SpeechRecognizer.ERROR_NO_MATCH
                            || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                    try {
                        speechRecognizer?.startListening(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restart: ${e.message}")
                        _isListening.value = false
                    }
                } else {
                    _isListening.value = false
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val newText = matches[0]
                    accumulatedText = if (accumulatedText.isEmpty()) {
                        newText
                    } else {
                        "$accumulatedText $newText"
                    }
                    trySend(Pair(accumulatedText, true))
                    Log.d(TAG, "Final: $newText (accumulated: ${accumulatedText.length} chars)")
                }

                // Auto-restart for continuous listening
                if (shouldRestart) {
                    try {
                        speechRecognizer?.startListening(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restart: ${e.message}")
                        _isListening.value = false
                    }
                } else {
                    _isListening.value = false
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partial = matches[0]
                    val fullText = if (accumulatedText.isEmpty()) {
                        partial
                    } else {
                        "$accumulatedText $partial"
                    }
                    trySend(Pair(fullText, false))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)
        speechRecognizer?.startListening(intent)

        awaitClose {
            shouldRestart = false
            stopListening()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
        }
        speechRecognizer = null
        _isListening.value = false
    }

    fun release() {
        stopListening()
    }
}
