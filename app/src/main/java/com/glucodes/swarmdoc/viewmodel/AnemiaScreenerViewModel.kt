package com.glucodes.swarmdoc.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.ml.vision.ConjunctivalPallorAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AnemiaUiState {
    object Idle : AnemiaUiState()
    object Analyzing : AnemiaUiState()
    data class Result(
        val severity: ConjunctivalPallorAnalyzer.Result.Severity,
        val hbRange: String,
        val confidence: Float
    ) : AnemiaUiState()
    data class Error(val message: String) : AnemiaUiState()
}

@HiltViewModel
class AnemiaScreenerViewModel @Inject constructor(
    private val analyzer: ConjunctivalPallorAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnemiaUiState>(AnemiaUiState.Idle)
    val uiState: StateFlow<AnemiaUiState> = _uiState.asStateFlow()

    /**
     * Analyze a captured image of the lower eyelid.
     * The input bitmap should ideally be cropped to the ROI containing the conjunctiva.
     */
    fun analyzeImage(bitmap: Bitmap) {
        if (_uiState.value is AnemiaUiState.Analyzing) return

        viewModelScope.launch {
            _uiState.value = AnemiaUiState.Analyzing

            // Run intensive color analysis on background thread
            val result = withContext(Dispatchers.Default) {
                analyzer.analyze(bitmap)
            }

            if (result.confidence < 0.2f || result.estimatedHbRange == "Invalid") {
                _uiState.value = AnemiaUiState.Error("Image too dark, blurry, or poor lighting. Please capture again.")
            } else {
                _uiState.value = AnemiaUiState.Result(
                    severity = result.severity,
                    hbRange = result.estimatedHbRange,
                    confidence = result.confidence
                )
            }
        }
    }

    /**
     * Run a demo analysis with simulated results when camera is unavailable (emulator).
     */
    fun runDemoAnalysis() {
        if (_uiState.value is AnemiaUiState.Analyzing) return

        viewModelScope.launch {
            _uiState.value = AnemiaUiState.Analyzing
            kotlinx.coroutines.delay(2000) // Simulate processing
            _uiState.value = AnemiaUiState.Result(
                severity = ConjunctivalPallorAnalyzer.Result.Severity.MILD_PALLOR,
                hbRange = "9-11 g/dL",
                confidence = 0.78f
            )
        }
    }

    fun reset() {
        _uiState.value = AnemiaUiState.Idle
    }
}
