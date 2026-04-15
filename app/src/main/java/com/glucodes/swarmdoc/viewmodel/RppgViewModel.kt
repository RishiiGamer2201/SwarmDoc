package com.glucodes.swarmdoc.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.ml.vision.RppgProcessor
import com.glucodes.swarmdoc.ml.vision.CapillaryRefillProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RppgUiState {
    object Idle : RppgUiState()
    data class Capturing(val progress: Float) : RppgUiState() // 0.0 to 1.0 (over 15s)
    object Processing : RppgUiState()
    data class Result(val heartRate: Int, val confidence: Float) : RppgUiState()
    data class Error(val message: String) : RppgUiState()
}

@HiltViewModel
class RppgViewModel @Inject constructor(
    private val rppgProcessor: RppgProcessor,
    private val crtProcessor: CapillaryRefillProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow<RppgUiState>(RppgUiState.Idle)
    val uiState: StateFlow<RppgUiState> = _uiState.asStateFlow()

    private val captureDurationMs = 15000L
    private var startTime = 0L

    fun startCapture() {
        if (_uiState.value !is RppgUiState.Idle) return
        
        viewModelScope.launch {
            rppgProcessor.reset()
            startTime = System.currentTimeMillis()
            _uiState.value = RppgUiState.Capturing(0f)
        }
    }

    fun processFrame(bitmap: Bitmap, timestamp: Long) {
        if (_uiState.value !is RppgUiState.Capturing) return

        val elapsed = timestamp - startTime
        val progress = (elapsed.toFloat() / captureDurationMs).coerceIn(0f, 1f)
        
        viewModelScope.launch {
            rppgProcessor.processFrame(bitmap)
        }
        
        if (progress >= 1f) {
            _uiState.value = RppgUiState.Processing
            finishCapture()
        } else {
            _uiState.value = RppgUiState.Capturing(progress)
        }
    }

    private fun finishCapture() {
        viewModelScope.launch {
            val bpm = rppgProcessor.computeHeartRate() // Returns an Int
            if (bpm > 0) {
                _uiState.value = RppgUiState.Result(bpm, 0.95f) // Hardcode confidence for now
            } else {
                _uiState.value = RppgUiState.Error("Face not detected clearly or too much movement.")
                delay(3000)
                _uiState.value = RppgUiState.Idle
            }
        }
    }
    
    fun reset() {
        _uiState.value = RppgUiState.Idle
    }

    /**
     * Run demo rPPG capture with simulated results when camera is unavailable (emulator).
     */
    fun runDemoCapture() {
        if (_uiState.value !is RppgUiState.Idle) return

        viewModelScope.launch {
            _uiState.value = RppgUiState.Capturing(0f)
            // Simulate 15 second capture with progress updates
            for (i in 1..15) {
                delay(300) // Speed up for demo
                _uiState.value = RppgUiState.Capturing(i / 15f)
            }
            _uiState.value = RppgUiState.Processing
            delay(1500) // Simulate FFT processing
            _uiState.value = RppgUiState.Result(heartRate = 72, confidence = 0.88f)
        }
    }

    // --- Capillary Refill logic interleaved here for simplicity since both are vision tools ---
    
    private val _crtState = MutableStateFlow<CapillaryRefillProcessor.State>(CapillaryRefillProcessor.State.IDLE)
    val crtState: StateFlow<CapillaryRefillProcessor.State> = _crtState.asStateFlow()
    
    private val _crtResult = MutableStateFlow<CapillaryRefillProcessor.Result?>(null)
    val crtResult: StateFlow<CapillaryRefillProcessor.Result?> = _crtResult.asStateFlow()
    
    fun processCapillaryFrame(bitmap: Bitmap, timestamp: Long) {
        val (state, result) = crtProcessor.processFrame(bitmap, timestamp)
        _crtState.value = state
        if (result != null) {
            _crtResult.value = result
        }
    }
    
    fun resetCapillary() {
        crtProcessor.reset()
        _crtState.value = CapillaryRefillProcessor.State.IDLE
        _crtResult.value = null
    }

    /**
     * Run demo capillary refill test with simulated results when camera is unavailable.
     */
    fun runDemoCapillary() {
        viewModelScope.launch {
            _crtState.value = CapillaryRefillProcessor.State.BLANCHED
            delay(1500)
            _crtState.value = CapillaryRefillProcessor.State.REFILLING
            delay(1500)
            _crtState.value = CapillaryRefillProcessor.State.COMPLETE
            _crtResult.value = CapillaryRefillProcessor.Result(
                refillTimeMs = 1800L,
                status = "Normal",
                confidence = 0.85f
            )
        }
    }
}
