package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.repository.PatientRepository
import com.glucodes.swarmdoc.domain.models.CommunityContext
import com.glucodes.swarmdoc.domain.models.ConditionResult
import com.glucodes.swarmdoc.domain.models.Diagnosis
import com.glucodes.swarmdoc.domain.models.RiskLevel
import com.glucodes.swarmdoc.mesh.sync.MeshSyncManager
import com.glucodes.swarmdoc.ml.triage.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val triageEngine: TriageInferenceEngine,
    private val meshSyncManager: MeshSyncManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val consultationId: Long = savedStateHandle.get<Long>("consultationId") ?: 0L

    private val _uiState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.Loading)
    val uiState: StateFlow<DiagnosisUiState> = _uiState.asStateFlow()

    init {
        if (consultationId != 0L) {
            analyzeConsultation(consultationId)
        } else {
            _uiState.value = DiagnosisUiState.Error("Invalid consultation ID")
        }
    }

    private fun analyzeConsultation(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = DiagnosisUiState.Loading

                val consultation = patientRepository.getConsultationById(id)
                if (consultation == null) {
                    _uiState.value = DiagnosisUiState.Error("Consultation not found")
                    return@launch
                }

                val patient = patientRepository.getPatientById(consultation.patientId)
                if (patient == null) {
                    _uiState.value = DiagnosisUiState.Error("Patient not found")
                    return@launch
                }

                val symptoms: List<String> = try {
                    json.decodeFromString(consultation.symptomsJson)
                } catch (e: Exception) {
                    emptyList()
                }

                // Build SymptomVector for new triage engine
                val ageGroup = when {
                    patient.age <= 5 -> AgeGroup.INFANT
                    patient.age <= 17 -> AgeGroup.CHILD
                    patient.age <= 55 -> AgeGroup.ADULT
                    else -> AgeGroup.ELDER
                }
                val sex = when (patient.sex) {
                    "FEMALE" -> SexType.FEMALE
                    "OTHER" -> SexType.OTHER
                    else -> SexType.MALE
                }

                val vector = SymptomVector(
                    ageGroup = ageGroup,
                    age = patient.age,
                    sex = sex,
                    symptomFlags = BooleanArray(symptoms.size) { true },
                    symptomNames = symptoms,
                    durationDays = consultation.durationDays,
                    temperature = consultation.temperature,
                )

                val result = triageEngine.infer(vector)

                // Fetch Mesh Community Context
                val zone = "PURI_ZONE_A"
                val clusters = meshSyncManager.detectClusters(zone)

                val topCondition = result.topConditions.firstOrNull()?.conditionEnglish ?: ""
                val matchingCluster = clusters.find { it.condition == topCondition }

                val communityContext = if (matchingCluster != null) {
                    CommunityContext(
                        similarCasesCount = matchingCluster.caseCount,
                        clusterDetected = true,
                        clusterMessage = matchingCluster.message,
                        affectedWorkers = 2
                    )
                } else {
                    CommunityContext(
                        similarCasesCount = 0,
                        clusterDetected = false,
                        clusterMessage = "No cluster detected in your area."
                    )
                }

                val finalRiskLevel = when {
                    matchingCluster?.isCritical == true -> RiskLevel.EMERGENCY
                    result.riskLevel == com.glucodes.swarmdoc.ml.triage.RiskLevel.EMERGENCY -> RiskLevel.EMERGENCY
                    result.riskLevel == com.glucodes.swarmdoc.ml.triage.RiskLevel.URGENT -> RiskLevel.URGENT
                    else -> RiskLevel.NORMAL
                }

                val diagnosis = Diagnosis(
                    conditions = result.topConditions.map {
                        ConditionResult(
                            nameEnglish = it.conditionEnglish,
                            nameLocal = it.conditionLocal,
                            confidence = it.confidence,
                            description = it.description
                        )
                    },
                    riskLevel = finalRiskLevel,
                    communityContext = communityContext
                )

                // Save results back to consultation
                val updatedConsultation = consultation.copy(
                    diagnosisJson = json.encodeToString(diagnosis),
                    riskLevel = finalRiskLevel.name,
                    communityContextJson = json.encodeToString(communityContext)
                )
                patientRepository.insertConsultation(updatedConsultation)

                _uiState.value = DiagnosisUiState.Success(
                    diagnosis = diagnosis,
                    patientName = patient.name,
                    symptoms = symptoms
                )

            } catch (e: Exception) {
                _uiState.value = DiagnosisUiState.Error(e.message ?: "Failed to analyze")
            }
        }
    }
}

sealed class DiagnosisUiState {
    object Loading : DiagnosisUiState()
    data class Success(
        val diagnosis: Diagnosis,
        val patientName: String,
        val symptoms: List<String>
    ) : DiagnosisUiState()
    data class Error(val message: String) : DiagnosisUiState()
}
