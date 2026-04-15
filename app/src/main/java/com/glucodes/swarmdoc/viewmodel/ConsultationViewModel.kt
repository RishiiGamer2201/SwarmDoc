package com.glucodes.swarmdoc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.local.entities.*
import com.glucodes.swarmdoc.data.repository.*
import com.glucodes.swarmdoc.ml.triage.*
import com.glucodes.swarmdoc.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ConsultationUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 5,
    // Step 1: Patient Basics
    val patientName: String = "",
    val patientAge: Int = 30,
    val patientSex: String = "MALE",
    val village: String = "",
    val district: String = "",
    val privacyMode: Boolean = false,
    val existingPatientId: Long? = null,
    // Step 2: Input modes
    val voiceCompleted: Boolean = false,
    val textCompleted: Boolean = false,
    val photoCompleted: Boolean = false,
    val transcribedText: String = "",
    val capturedPhotoPath: String = "",
    // Step 3: Symptoms
    val selectedSymptoms: Set<String> = emptySet(),
    val durationDays: Int = 1,
    val temperature: String = "",
    val systolicBP: String = "",
    val diastolicBP: String = "",
    val pulseRate: String = "",
    val spo2: String = "",
    val additionalNotes: String = "",
    val expandedWomensHealth: Boolean = false,
    val expandedAdvancedChecks: Boolean = false,
    // Step 4: Diagnosis
    val isAnalyzing: Boolean = false,
    val diagnosisResult: DiagnosisResult? = null,
    val communityClusterDetected: Boolean = false,
    val clusterMessage: String = "",
    val clusterCaseCount: Int = 0,
    // Step 5: Action
    val recommendedMedicines: List<MedicineRecommendation> = emptyList(),
    val isSaved: Boolean = false,
    val savedConsultationId: Long = 0,
    // Loading
    val isLoading: Boolean = false,
    val errorMessage: String = "",
)

data class MedicineRecommendation(
    val genericName: String,
    val localName: String,
    val dose: String,
    val category: String,
    val inStock: Boolean = true,
    val prescriptionRequired: Boolean = false,
)

@HiltViewModel
class ConsultationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patientRepository: PatientRepository,
    private val meshRepository: MeshRepository,
    private val meshSyncManager: com.glucodes.swarmdoc.mesh.sync.MeshSyncManager,
    private val medicineRepository: MedicineRepository,
    private val settingsRepository: SettingsRepository,
    private val triageEngine: TriageInferenceEngine,
    private val onnxTriageModel: OnnxTriageModel,
) : ViewModel() {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(ConsultationUiState())
    val state: StateFlow<ConsultationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val district = settingsRepository.district.first()
            _state.update { it.copy(district = district) }
        }
    }

    fun updatePatientName(name: String) { _state.update { it.copy(patientName = name) } }
    fun updatePatientAge(age: Int) { _state.update { it.copy(patientAge = age) } }
    fun updatePatientSex(sex: String) { _state.update { it.copy(patientSex = sex) } }
    fun updateVillage(village: String) { _state.update { it.copy(village = village) } }
    fun updateDistrict(district: String) { _state.update { it.copy(district = district) } }
    fun togglePrivacyMode(enabled: Boolean) { _state.update { it.copy(privacyMode = enabled) } }

    fun updateTranscribedText(text: String) { _state.update { it.copy(transcribedText = text) } }
    fun markVoiceCompleted() { _state.update { it.copy(voiceCompleted = true) } }
    fun markTextCompleted() { _state.update { it.copy(textCompleted = true) } }
    fun markPhotoCompleted(path: String) { _state.update { it.copy(photoCompleted = true, capturedPhotoPath = path) } }

    fun toggleSymptom(symptom: String) {
        _state.update {
            val newSet = it.selectedSymptoms.toMutableSet()
            if (symptom in newSet) newSet.remove(symptom) else newSet.add(symptom)
            it.copy(selectedSymptoms = newSet)
        }
    }
    fun updateDuration(days: Int) { _state.update { it.copy(durationDays = days) } }
    fun updateTemperature(temp: String) { _state.update { it.copy(temperature = temp) } }
    fun updateSystolicBP(bp: String) { _state.update { it.copy(systolicBP = bp) } }
    fun updateDiastolicBP(bp: String) { _state.update { it.copy(diastolicBP = bp) } }
    fun updatePulseRate(rate: String) { _state.update { it.copy(pulseRate = rate) } }
    fun updateSpo2(spo2: String) { _state.update { it.copy(spo2 = spo2) } }
    fun updateNotes(notes: String) { _state.update { it.copy(additionalNotes = notes) } }
    fun toggleWomensHealth() { _state.update { it.copy(expandedWomensHealth = !it.expandedWomensHealth) } }
    fun toggleAdvancedChecks() { _state.update { it.copy(expandedAdvancedChecks = !it.expandedAdvancedChecks) } }

    fun goToStep(step: Int) { _state.update { it.copy(currentStep = step.coerceIn(1, 5)) } }
    fun nextStep() { _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(5)) } }
    fun previousStep() { _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) } }

    fun getSymptomListForPatient(): List<String> {
        val s = _state.value
        val ageGroup = Constants.getAgeGroup(s.patientAge)
        val baseSymptoms = when (ageGroup) {
            Constants.AGE_GROUP_INFANT -> Constants.INFANT_SYMPTOMS
            Constants.AGE_GROUP_CHILD -> Constants.CHILD_SYMPTOMS
            Constants.AGE_GROUP_ELDER -> Constants.ELDER_SYMPTOMS
            else -> Constants.ADULT_SYMPTOMS
        }
        return baseSymptoms
    }

    fun runDiagnosis() {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, currentStep = 4) }

            try {
                val s = _state.value
                val ageGroup = when {
                    s.patientAge <= 5 -> AgeGroup.INFANT
                    s.patientAge <= 17 -> AgeGroup.CHILD
                    s.patientAge <= 55 -> AgeGroup.ADULT
                    else -> AgeGroup.ELDER
                }
                val sex = when (s.patientSex) {
                    "FEMALE" -> SexType.FEMALE
                    "OTHER" -> SexType.OTHER
                    else -> SexType.MALE
                }

                val symptomList = s.selectedSymptoms.toList()
                val vector = SymptomVector(
                    ageGroup = ageGroup,
                    age = s.patientAge,
                    sex = sex,
                    symptomFlags = BooleanArray(symptomList.size) { true },
                    symptomNames = symptomList,
                    durationDays = s.durationDays,
                    temperature = s.temperature.toFloatOrNull(),
                    systolicBP = s.systolicBP.toIntOrNull(),
                    diastolicBP = s.diastolicBP.toIntOrNull(),
                    pulseRate = s.pulseRate.toIntOrNull(),
                    spo2 = s.spo2.toIntOrNull(),
                )

                var result: DiagnosisResult? = null

                // 1. Try fully offline ONNX Inference model first
                try {
                    val features = FloatArray(34) { 0f }
                    // Map vector into features [age(1), sex(2), temp(1), symptoms(30)]
                    features[0] = s.patientAge.toFloat() / 100f // Normalized age
                    features[1] = if (sex == SexType.MALE) 1f else 0f
                    features[2] = if (sex == SexType.FEMALE) 1f else 0f
                    features[3] = (s.temperature.toFloatOrNull() ?: 37.0f) / 40.0f
                    
                    // Simple map for 30 base symptoms from Constants
                    val allSymptoms = Constants.ADULT_SYMPTOMS // Using adult baseline as complete list
                    for ((index, sym) in allSymptoms.withIndex()) {
                        if (index < 30) {
                            features[4 + index] = if (sym in s.selectedSymptoms) 1f else 0f
                        }
                    }

                    val probs = onnxTriageModel.infer(features)
                    if (probs != null && probs.size >= 3) { // Expecting at least 3 classes
                        // Simulated mapping of neural net outputs back to Risk levels and conditions
                        val topClassIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
                        val confidence = probs[topClassIdx]
                        
                        // Let's assume standard index mappings for demonstration:
                        val mappedCondition = when (topClassIdx) {
                            0 -> "Dengue Fever"
                            1 -> "Malaria"
                            2 -> "Typhoid"
                            3 -> "Pneumonia"
                            else -> "Viral Syndrome"
                        }
                        
                        result = DiagnosisResult(
                            topConditions = listOf(
                                ConditionScore(mappedCondition, mappedCondition, confidence, RiskLevel.URGENT, "AI Model Output")
                            ),
                            riskLevel = if (confidence > 0.8f) RiskLevel.EMERGENCY else RiskLevel.URGENT,
                            recommendedAction = "Refer to health facility for confirmation."
                        )
                        android.util.Log.d("Consultation", "ML Inference SUCCESS! Result: $mappedCondition")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("Consultation", "ONNX Triage Failed. Falling back to DecisionTree. Error: \${e.message}")
                }

                // 2. Fallback to DecisionTree Engine
                if (result == null) {
                    result = triageEngine.infer(vector)
                }

                // Check community context
                val weekAgo = System.currentTimeMillis() - 7 * 86400000L
                val recentPackets = meshRepository.getRecentPackets(weekAgo)
                val matchingPackets = recentPackets.filter { pkt ->
                    val pktSymptoms = try {
                        json.decodeFromString<List<String>>(pkt.anonymizedSymptomsJson)
                    } catch (_: Exception) { emptyList() }
                    pktSymptoms.any { it in symptomList }
                }
                val clusterDetected = matchingPackets.sumOf { it.caseCount } >= 3
                val clusterCount = matchingPackets.sumOf { it.caseCount }

                _state.update {
                    it.copy(
                        isAnalyzing = false,
                        diagnosisResult = result,
                        communityClusterDetected = clusterDetected,
                        clusterMessage = if (clusterDetected) "$clusterCount similar cases detected in your area this week - possible outbreak" else "",
                        clusterCaseCount = clusterCount,
                    )
                }

                // Load medicine recommendations
                if (result != null) {
                    loadMedicineRecommendations(result, ageGroup)
                }

            } catch (e: Exception) {
                _state.update { it.copy(isAnalyzing = false, errorMessage = "Diagnosis failed: ${e.message}") }
            }
        }
    }

    private suspend fun loadMedicineRecommendations(result: DiagnosisResult, ageGroup: AgeGroup) {
        val topCondition = result.topConditions.firstOrNull() ?: return
        val mappings = medicineRepository.getMedicinesForCondition(topCondition.conditionEnglish)

        val recommendations = mappings.take(3).map { mapping ->
            val medicine = medicineRepository.getMedicineByName(mapping.medicineGenericName)
            val inventory = medicine?.let { medicineRepository.getInventoryForMedicine(it.id) }
            val dose = when (ageGroup) {
                AgeGroup.INFANT, AgeGroup.CHILD -> mapping.notesChild
                AgeGroup.ELDER -> mapping.notesElder
                else -> mapping.notesAdult
            }
            MedicineRecommendation(
                genericName = mapping.medicineGenericName,
                localName = medicine?.localName ?: mapping.medicineGenericName,
                dose = dose,
                category = medicine?.category ?: "",
                inStock = inventory?.inStock ?: true,
                prescriptionRequired = medicine?.prescriptionRequired ?: false,
            )
        }
        _state.update { it.copy(recommendedMedicines = recommendations) }
    }

    fun saveConsultation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val s = _state.value
                val workerName = settingsRepository.workerName.first()

                // Insert or reuse patient
                val patientId = s.existingPatientId ?: run {
                    val existing = if (s.patientName.isNotBlank()) {
                        patientRepository.findPatientByNameAndDistrict(s.patientName, s.district)
                    } else null

                    existing?.id ?: patientRepository.insertPatient(
                        PatientEntity(
                            name = s.patientName.ifBlank { "Patient" },
                            initials = s.patientName.split(" ").map { it.firstOrNull()?.uppercase() ?: "" }.joinToString(""),
                            age = s.patientAge,
                            sex = s.patientSex,
                            village = s.village,
                            district = s.district,
                        )
                    )
                }

                val topDiag = s.diagnosisResult?.topConditions?.firstOrNull()
                val inputModes = buildList {
                    if (s.textCompleted) add("TEXT")
                    if (s.voiceCompleted) add("VOICE")
                    if (s.photoCompleted) add("PHOTO")
                }.joinToString(",")

                val consultation = ConsultationEntity(
                    patientId = patientId,
                    workerId = workerName,
                    symptomsJson = json.encodeToString(s.selectedSymptoms.toList()),
                    additionalNotes = s.additionalNotes,
                    durationDays = s.durationDays,
                    temperature = s.temperature.toFloatOrNull(),
                    vitalsJson = json.encodeToString(mapOf(
                        "systolicBP" to (s.systolicBP.toIntOrNull()?.toString() ?: ""),
                        "diastolicBP" to (s.diastolicBP.toIntOrNull()?.toString() ?: ""),
                        "pulseRate" to (s.pulseRate.toIntOrNull()?.toString() ?: ""),
                        "spo2" to (s.spo2.toIntOrNull()?.toString() ?: ""),
                    )),
                    diagnosisJson = json.encodeToString(s.diagnosisResult?.topConditions?.map {
                        mapOf("name" to it.conditionEnglish, "confidence" to it.confidence.toString(), "risk" to it.riskLevel.name)
                    } ?: emptyList<Map<String, String>>()),
                    riskLevel = s.diagnosisResult?.riskLevel?.name ?: "NORMAL",
                    communityContextJson = if (s.communityClusterDetected) json.encodeToString(mapOf(
                        "clusterDetected" to "true", "caseCount" to s.clusterCaseCount.toString(), "message" to s.clusterMessage
                    )) else "{}",
                    inputMode = inputModes.split(",").firstOrNull() ?: "TEXT",
                    inputModesUsed = inputModes,
                    ageAtVisit = s.patientAge,
                    sex = s.patientSex,
                    topDiagnosis = topDiag?.conditionEnglish ?: "",
                    diagnosisConfidence = topDiag?.confidence ?: 0f,
                    recommendedAction = s.diagnosisResult?.recommendedAction ?: "",
                    medicinesRecommendedJson = json.encodeToString(s.recommendedMedicines.map { it.genericName }),
                    photoFilePathsJson = if (s.capturedPhotoPath.isNotBlank()) json.encodeToString(listOf(s.capturedPhotoPath)) else "[]",
                    rawTranscript = s.transcribedText,
                )

                val consultationId = patientRepository.insertConsultation(consultation)

                // Save symptom entries
                val entries = s.selectedSymptoms.map { symptom ->
                    SymptomEntryEntity(consultationId = consultationId, symptomName = symptom, duration = s.durationDays)
                }
                patientRepository.insertSymptomEntries(entries)

                // Auto-create Mesh Sync Packet to contribute to Village Health Graph
                try {
                    val workerHash = workerName.hashCode().toString()
                    val packet = meshSyncManager.createPacket(
                        workerIdHash = workerHash,
                        symptoms = s.selectedSymptoms.toList(),
                        conditionSuspected = topDiag?.conditionEnglish ?: "",
                        caseCount = 1,
                        gpsZone = s.district
                    )
                    // Save to local mesh repo (which will be advertised via BLE later)
                    meshSyncManager.mergePackets(listOf(packet))
                } catch (e: Exception) {
                    // Non-fatal, just log
                    e.printStackTrace()
                }

                _state.update { it.copy(isLoading = false, isSaved = true, savedConsultationId = consultationId, currentStep = 5) }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun generateShareText(): String {
        val s = _state.value
        val topDiag = s.diagnosisResult?.topConditions?.firstOrNull()
        return buildString {
            appendLine("=== SwarmDoc Patient Report ===")
            appendLine("Patient: ${s.patientName}")
            appendLine("Age: ${s.patientAge} | Sex: ${s.patientSex}")
            appendLine("Village: ${s.village}, ${s.district}")
            appendLine("---")
            appendLine("Symptoms: ${s.selectedSymptoms.joinToString(", ") { Constants.SYMPTOM_DISPLAY_NAMES[it] ?: it }}")
            appendLine("Duration: ${s.durationDays} days")
            if (s.temperature.isNotBlank()) appendLine("Temperature: ${s.temperature}C")
            appendLine("---")
            appendLine("Diagnosis: ${topDiag?.conditionEnglish ?: "Pending"}")
            appendLine("Risk Level: ${s.diagnosisResult?.riskLevel?.name ?: "NORMAL"}")
            appendLine("Confidence: ${((topDiag?.confidence ?: 0f) * 100).toInt()}%")
            appendLine("---")
            appendLine("Recommended Action: ${s.diagnosisResult?.recommendedAction ?: "N/A"}")
            appendLine("Medicines: ${s.recommendedMedicines.joinToString(", ") { "${it.genericName} (${it.dose})" }}")
            if (s.communityClusterDetected) {
                appendLine("---")
                appendLine("ALERT: ${s.clusterMessage}")
            }
            appendLine("---")
            appendLine("ASHA Worker: ${s.district}")
            appendLine("Generated by SwarmDoc (Aarogya Jaal)")
        }
    }
}
