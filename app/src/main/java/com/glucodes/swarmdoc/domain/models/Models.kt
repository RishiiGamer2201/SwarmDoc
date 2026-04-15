package com.glucodes.swarmdoc.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Patient(
    val id: Long = 0,
    val name: String = "",
    val initials: String = "",
    val age: Int = 0,
    val sex: Sex = Sex.MALE,
    val village: String = "",
    val district: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
enum class Sex {
    MALE, FEMALE, OTHER
}

@Serializable
data class Diagnosis(
    val conditions: List<ConditionResult> = emptyList(),
    val riskLevel: RiskLevel = RiskLevel.NORMAL,
    val communityContext: CommunityContext? = null,
    val recommendedActions: List<String> = emptyList(),
)

@Serializable
data class ConditionResult(
    val nameEnglish: String,
    val nameLocal: String,
    val confidence: Float,
    val description: String,
)

@Serializable
enum class RiskLevel {
    EMERGENCY, URGENT, NORMAL
}

@Serializable
data class CommunityContext(
    val similarCasesCount: Int = 0,
    val clusterDetected: Boolean = false,
    val clusterMessage: String = "",
    val affectedWorkers: Int = 0,
)

@Serializable
data class SymptomInput(
    val symptoms: List<String> = emptyList(),
    val additionalNotes: String = "",
    val durationDays: Int = 1,
    val temperature: Float? = null,
    val recentTravel: Boolean = false,
)

@Serializable
data class VitalsInput(
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val pulseRate: Int? = null,
    val spo2: Int? = null,
    val weight: Float? = null,
)

@Serializable
data class ConsultationData(
    val id: Long = 0,
    val patientId: Long = 0,
    val symptoms: SymptomInput = SymptomInput(),
    val vitals: VitalsInput = VitalsInput(),
    val diagnosis: Diagnosis = Diagnosis(),
    val timestamp: Long = System.currentTimeMillis(),
    val inputMode: InputMode = InputMode.TEXT,
)

@Serializable
enum class InputMode {
    TEXT, VOICE, PHOTO, VOICE_NOTE
}

@Serializable
data class WorkerProfile(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val district: String = "",
    val subDistrict: String = "",
    val gpsZone: String = "",
    val pin: String = "",
)

@Serializable
data class PHCInfo(
    val name: String,
    val district: String,
    val state: String,
    val lat: Double,
    val lon: Double,
    val phone: String = "",
)

@Serializable
data class ASHAWorker(
    val id: String,
    val name: String,
    val phone: String,
    val gpsZone: String,
    val district: String,
    val isActive: Boolean = true,
)

@Serializable
data class MeshPacket(
    val workerIdHash: String,
    val symptomVector: List<String>,
    val timestamp: Long,
    val gpsZone: String,
    val caseCount: Int,
    val conditionSuspected: String = "",
)

@Serializable
data class SymptomCluster(
    val symptomName: String,
    val caseCount: Int,
    val trendUp: Boolean,
    val crossesThreshold: Boolean,
)

@Serializable
data class DistrictInfo(
    val name: String,
    val state: String,
    val subDistricts: List<String>,
)
