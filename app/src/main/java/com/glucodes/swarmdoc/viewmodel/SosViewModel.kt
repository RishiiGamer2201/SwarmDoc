package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.repository.DoctorRepository
import com.glucodes.swarmdoc.ui.screens.SosPhase
import com.glucodes.swarmdoc.ui.screens.SosUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SosViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SosUiState())
    val state: StateFlow<SosUiState> = _state.asStateFlow()

    fun startSos() {
        _state.update { it.copy(phase = SosPhase.CONFIRMING) }
    }

    fun cancelSos() {
        _state.update { SosUiState() }
    }

    fun resetSos() {
        _state.update { SosUiState() }
    }

    fun confirmSos() {
        _state.update { it.copy(phase = SosPhase.ALERTING) }

        viewModelScope.launch {
            // Simulate alert delay
            delay(2000)

            // Find nearest available doctor
            val freeDoctors = doctorRepository.getFreeDoctors()
            val busyLowRisk = doctorRepository.getBusyLowRiskDoctors()

            if (freeDoctors.isNotEmpty()) {
                val doctor = freeDoctors.first()
                _state.update {
                    it.copy(
                        phase = SosPhase.RESULT,
                        assignedDoctorName = doctor.name,
                        assignedDoctorPhone = doctor.phone,
                        assignedPhcName = doctor.phcName,
                        resultMessage = "${doctor.name} at ${doctor.phcName} has been alerted. They will attend to your patient shortly.",
                        patientRiskLevel = "EMERGENCY",
                        wasReassigned = false,
                    )
                }
            } else if (busyLowRisk.isNotEmpty()) {
                val doctor = busyLowRisk.first()
                // Trigger reassignment
                val updated = doctor.copy(reassignmentPending = true)
                doctorRepository.updateDoctor(updated)

                _state.update {
                    it.copy(
                        phase = SosPhase.RESULT,
                        assignedDoctorName = doctor.name,
                        assignedDoctorPhone = doctor.phone,
                        assignedPhcName = doctor.phcName,
                        resultMessage = "${doctor.name} is finishing a low-priority case and will be with your patient shortly. Estimated wait: 15 minutes.",
                        patientRiskLevel = "EMERGENCY",
                        wasReassigned = true,
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        phase = SosPhase.RESULT,
                        assignedDoctorName = "No doctor available",
                        assignedPhcName = "All PHCs busy",
                        resultMessage = "All doctors are currently handling high-risk cases. Alert has been escalated to district health office.",
                        patientRiskLevel = "EMERGENCY",
                    )
                }
            }
        }
    }
}
