package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.local.entities.ConsultationEntity
import com.glucodes.swarmdoc.data.local.entities.PatientEntity
import com.glucodes.swarmdoc.data.local.entities.PatientWithConsultations
import com.glucodes.swarmdoc.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterRisk = MutableStateFlow<String?>(null)
    val filterRisk: StateFlow<String?> = _filterRisk.asStateFlow()

    val allPatients: StateFlow<List<PatientWithConsultations>> = patientRepository.getAllPatientsWithConsultations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPatients: StateFlow<List<PatientWithConsultations>> = combine(allPatients, _searchQuery, _filterRisk) { patients, query, risk ->
        var filtered = patients

        if (query.isNotBlank()) {
            filtered = filtered.filter { 
                it.patient.name.contains(query, ignoreCase = true) || 
                it.patient.initials.contains(query, ignoreCase = true)
            }
        }

        if (risk != null) {
            filtered = filtered.filter { record -> 
                record.consultations.maxByOrNull { it.timestamp }?.riskLevel == risk
            }
        }
        
        filtered.sortedByDescending { record -> 
            record.consultations.maxByOrNull { it.timestamp }?.timestamp ?: 0L 
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPatient = MutableStateFlow<PatientWithConsultations?>(null)
    val selectedPatient: StateFlow<PatientWithConsultations?> = _selectedPatient.asStateFlow()

    private val _selectedConsultation = MutableStateFlow<ConsultationEntity?>(null)
    val selectedConsultation: StateFlow<ConsultationEntity?> = _selectedConsultation.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterRisk(risk: String?) {
        _filterRisk.value = risk
    }

    fun loadPatient(patientId: Long) {
        viewModelScope.launch {
            _selectedPatient.value = patientRepository.getPatientWithConsultations(patientId)
        }
    }

    fun loadConsultation(consultationId: Long) {
        viewModelScope.launch {
            _selectedConsultation.value = patientRepository.getConsultationById(consultationId)
            val patientId = _selectedConsultation.value?.patientId
            if (patientId != null) {
                _selectedPatient.value = patientRepository.getPatientWithConsultations(patientId)
            }
        }
    }
}
