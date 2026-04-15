package com.glucodes.swarmdoc.data.local.entities

import androidx.room.*

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val initials: String = "",
    val age: Int = 0,
    val sex: String = "MALE",
    val village: String = "",
    val district: String = "",
    val photoPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "consultations",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("patientId")]
)
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long = 0,
    val workerId: String = "",
    val symptomsJson: String = "[]",
    val additionalNotes: String = "",
    val durationDays: Int = 1,
    val temperature: Float? = null,
    val recentTravel: Boolean = false,
    val vitalsJson: String = "{}",
    val diagnosisJson: String = "{}",
    val riskLevel: String = "NORMAL",
    val communityContextJson: String = "{}",
    val inputMode: String = "TEXT",
    val inputModesUsed: String = "",
    val ageAtVisit: Int = 0,
    val sex: String = "",
    val focusBranchUsed: String = "",
    val topDiagnosis: String = "",
    val diagnosisConfidence: Float = 0f,
    val recommendedAction: String = "",
    val medicinesRecommendedJson: String = "[]",
    val wasReferred: Boolean = false,
    val referralPhcName: String = "",
    val photoFilePathsJson: String = "[]",
    val voiceNoteFilePath: String = "",
    val rawTranscript: String = "",
    val structuredSummary: String = "",
    val wasSosTriggered: Boolean = false,
    val sosOutcome: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val followUpDate: Long? = null,
    val isFlagged: Boolean = false,
)

@Entity(
    tableName = "symptom_entries",
    foreignKeys = [
        ForeignKey(
            entity = ConsultationEntity::class,
            parentColumns = ["id"],
            childColumns = ["consultationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("consultationId")]
)
data class SymptomEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val consultationId: Long = 0,
    val symptomName: String = "",
    val duration: Int = 0,
    val severity: String = "MODERATE",
)

@Entity(tableName = "mesh_sync_packets")
data class MeshSyncPacketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceWorkerIdHash: String = "",
    val anonymizedSymptomsJson: String = "[]",
    val conditionSuspected: String = "",
    val caseCount: Int = 1,
    val gpsZone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val syncedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "alert_records")
data class AlertRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val condition: String = "",
    val caseCount: Int = 0,
    val region: String = "",
    val isActive: Boolean = true,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val genericName: String = "",
    val localName: String = "",
    val category: String = "",
    val standardDoseAdult: String = "",
    val standardDoseChild: String = "",
    val standardDoseElder: String = "",
    val contraindications: String = "",
    val availableAtPhc: Boolean = true,
    val availableAtSubcentre: Boolean = false,
    val prescriptionRequired: Boolean = false,
)

@Entity(tableName = "medicine_inventory")
data class MedicineInventoryEntity(
    @PrimaryKey
    val medicineId: Long = 0,
    val inStock: Boolean = true,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val mfdDate: String = "",
    val expiryDate: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Entity(tableName = "condition_medicine_mappings")
data class ConditionMedicineMappingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conditionName: String = "",
    val medicineGenericName: String = "",
    val priority: Int = 1,
    val notesAdult: String = "",
    val notesChild: String = "",
    val notesElder: String = "",
)

@Entity(tableName = "doctor_registry")
data class DoctorRegistryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val phone: String = "",
    val phcName: String = "",
    val phcLat: Double = 0.0,
    val phcLng: Double = 0.0,
    val currentStatus: String = "FREE",
    val currentPatientId: Long? = null,
    val reassignmentPending: Boolean = false,
)

data class PatientWithConsultations(
    @Embedded val patient: PatientEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "patientId",
    )
    val consultations: List<ConsultationEntity>,
)

data class MedicineWithInventory(
    @Embedded val medicine: MedicineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicineId",
    )
    val inventory: MedicineInventoryEntity?,
)
