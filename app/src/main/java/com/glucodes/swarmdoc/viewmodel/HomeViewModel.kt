package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.local.entities.AlertRecordEntity
import com.glucodes.swarmdoc.data.repository.MeshRepository
import com.glucodes.swarmdoc.data.repository.PatientRepository
import com.glucodes.swarmdoc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val meshRepository: MeshRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val workerName: StateFlow<String> = settingsRepository.workerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ASHA Worker")

    val focusArea: StateFlow<String> = settingsRepository.focusArea
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General Health")

    private val _patientCount = MutableStateFlow(0)
    val patientCount: StateFlow<Int> = _patientCount

    private val _activeWorkers = MutableStateFlow(3)
    val activeWorkers: StateFlow<Int> = _activeWorkers

    val hasOutbreakAlert: StateFlow<Boolean> = meshRepository.getActiveAlerts()
        .map { alerts -> alerts.any { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _patientCount.value = patientRepository.getPatientCount()
        }
        viewModelScope.launch {
            _activeWorkers.value = meshRepository.getUniqueDeviceCount().coerceAtLeast(1)
        }
    }
}
