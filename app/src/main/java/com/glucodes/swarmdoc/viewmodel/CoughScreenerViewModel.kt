package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.ml.audio.AudioRecorder
import com.glucodes.swarmdoc.ml.audio.CoughClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CoughUiState {
    object Idle : CoughUiState()
    data class Recording(val durationMs: Long) : CoughUiState()
    object Analyzing : CoughUiState()
    data class Result(
        val isCoughDetected: Boolean,
        val coughType: String,
        val confidence: Float,
        val tbSuspected: Boolean,
        val recommendation: String = ""
    ) : CoughUiState()
    data class Error(val message: String) : CoughUiState()
}

@HiltViewModel
class CoughScreenerViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val classifier: CoughClassifier
) : ViewModel() {
    companion object {
        private const val MAX_RECORD_MS = 8000L
    }

    private val _uiState = MutableStateFlow<CoughUiState>(CoughUiState.Idle)
    val uiState: StateFlow<CoughUiState> = _uiState.asStateFlow()
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    // Expose amplitude from audio recorder for visualizer
    val amplitude: StateFlow<Float> = audioRecorder.amplitude

    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    private var shouldAnalyzeAfterStop = false

    /**
     * Start recording and then analyzing using the ONNX CoughClassifier.
     */
    fun startRecording() {
        if (!audioRecorder.hasPermission()) {
            _uiState.value = CoughUiState.Error("Microphone permission required")
            return
        }

        if (_uiState.value is CoughUiState.Recording || _uiState.value is CoughUiState.Analyzing) {
            return
        }

        shouldAnalyzeAfterStop = false
        _elapsedMs.value = 0L
        recordingJob = viewModelScope.launch {
            _uiState.value = CoughUiState.Recording(MAX_RECORD_MS)

            timerJob?.cancel()
            timerJob = launch {
                while (audioRecorder.isCurrentlyRecording() || _uiState.value is CoughUiState.Recording) {
                    delay(100L)
                    _elapsedMs.value = (_elapsedMs.value + 100L).coerceAtMost(MAX_RECORD_MS)
                }
            }

            val audioBuffer = audioRecorder.recordAudioRaw(MAX_RECORD_MS)
            timerJob?.cancel()
            timerJob = null

            if (!shouldAnalyzeAfterStop) {
                _uiState.value = CoughUiState.Idle
                return@launch
            }

            _uiState.value = CoughUiState.Analyzing

            if (audioBuffer.isEmpty()) {
                _uiState.value = CoughUiState.Error("Failed to record audio")
                return@launch
            }

            if (!classifier.isReady()) {
                classifier.initialize()
            }
            if (!classifier.isReady()) {
                _uiState.value = CoughUiState.Error("Cough model unavailable: ${classifier.getModelStatusMessage()}")
                return@launch
            }

            val result = classifier.classify(audioBuffer)
            if (
                result.pattern.contains("Model", ignoreCase = true) ||
                result.pattern.contains("Error", ignoreCase = true) ||
                result.pattern.contains("Low Signal", ignoreCase = true)
            ) {
                _uiState.value = CoughUiState.Error(result.recommendation)
                return@launch
            }

            _uiState.value = CoughUiState.Result(
                isCoughDetected = !result.pattern.contains("No Cough", ignoreCase = true) && result.confidence > 0.3f,
                coughType = result.pattern,
                confidence = result.confidence,
                tbSuspected = result.isTBSuspected,
                recommendation = result.recommendation
            )
        }
    }

    fun submitRecording() {
        shouldAnalyzeAfterStop = true
        audioRecorder.stopRecording()
    }

    fun rerecord() {
        shouldAnalyzeAfterStop = false
        audioRecorder.stopRecording()
        recordingJob?.cancel()
        timerJob?.cancel()
        _elapsedMs.value = 0L
        _uiState.value = CoughUiState.Idle
    }
    
    fun reset() {
        shouldAnalyzeAfterStop = false
        timerJob?.cancel()
        _elapsedMs.value = 0L
        _uiState.value = CoughUiState.Idle
    }

    /**
     * Run a demo analysis with simulated results when microphone is unavailable (emulator).
     */
    fun runDemoAnalysis() {
        if (_uiState.value is CoughUiState.Recording || _uiState.value is CoughUiState.Analyzing) return

        viewModelScope.launch {
            _uiState.value = CoughUiState.Recording(3000L)
            kotlinx.coroutines.delay(2000)
            _uiState.value = CoughUiState.Analyzing
            kotlinx.coroutines.delay(1500)
            _uiState.value = CoughUiState.Result(
                isCoughDetected = true,
                coughType = "Dry Cough",
                confidence = 0.82f,
                tbSuspected = false,
                recommendation = "No severe pathology detected. Ensure hydration."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.stopRecording()
        timerJob?.cancel()
    }
}
