package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.local.entities.ConsultationEntity
import com.glucodes.swarmdoc.data.local.entities.PatientEntity
import com.glucodes.swarmdoc.data.local.entities.SymptomEntryEntity
import com.glucodes.swarmdoc.data.repository.PatientRepository
import com.glucodes.swarmdoc.domain.models.InputMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class IntakeViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savedConsultationId = MutableStateFlow<Long?>(null)
    val savedConsultationId: StateFlow<Long?> = _savedConsultationId.asStateFlow()

    fun saveIntakeAndProceed(
        name: String,
        age: Int,
        sex: String,
        village: String,
        district: String,
        selectedSymptoms: List<String>,
        additionalSymptoms: String,
        durationDays: Int,
        temperature: Float?,
        recentTravel: Boolean,
        inputMode: InputMode = InputMode.TEXT,
    ) {
        viewModelScope.launch {
            _isSaving.value = true

            // Derive initials
            val initials = if (name.isNotBlank()) {
                name.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
            } else {
                "??"
            }

            // Save Patient
            val patientId = patientRepository.insertPatient(
                PatientEntity(
                    name = name.takeIf { it.isNotBlank() } ?: "Anonymous Patient",
                    initials = initials,
                    age = age,
                    sex = sex,
                    village = village,
                    district = district
                )
            )

            // Save Consultation (Pre-diagnosis)
            val consultationId = patientRepository.insertConsultation(
                ConsultationEntity(
                    patientId = patientId,
                    symptomsJson = json.encodeToString(selectedSymptoms),
                    additionalNotes = additionalSymptoms,
                    durationDays = durationDays,
                    temperature = temperature,
                    recentTravel = recentTravel,
                    inputMode = inputMode.name,
                    ageAtVisit = age,
                    sex = sex,
                )
            )

            // Save Symptom Entries
            val symptomEntries = selectedSymptoms.map { symptomName ->
                SymptomEntryEntity(
                    consultationId = consultationId,
                    symptomName = symptomName,
                    duration = durationDays,
                )
            }
            patientRepository.insertSymptomEntries(symptomEntries)

            _isSaving.value = false
            _savedConsultationId.value = consultationId
        }
    }
    
    fun resetState() {
        _savedConsultationId.value = null
        _isSaving.value = false
    }
}
