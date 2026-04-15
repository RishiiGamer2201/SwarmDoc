package com.glucodes.swarmdoc.data.local.dao

import androidx.room.*
import com.glucodes.swarmdoc.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientById(patientId: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE name LIKE '%' || :query || '%' OR initials LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Transaction
    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientWithConsultations(patientId: Long): PatientWithConsultations?

    @Transaction
    @Query("SELECT * FROM patients ORDER BY createdAt DESC")
    fun getAllPatientsWithConsultations(): Flow<List<PatientWithConsultations>>

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCount(): Int

    @Query("SELECT * FROM patients WHERE name = :name AND district = :district LIMIT 1")
    suspend fun findPatientByNameAndDistrict(name: String, district: String): PatientEntity?
}

@Dao
interface ConsultationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(consultation: ConsultationEntity): Long

    @Update
    suspend fun updateConsultation(consultation: ConsultationEntity)

    @Query("SELECT * FROM consultations WHERE id = :consultationId")
    suspend fun getConsultationById(consultationId: Long): ConsultationEntity?

    @Query("SELECT * FROM consultations WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getConsultationsForPatient(patientId: Long): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM consultations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConsultations(limit: Int = 10): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM consultations WHERE riskLevel = :riskLevel ORDER BY timestamp DESC")
    fun getConsultationsByRiskLevel(riskLevel: String): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM consultations WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getConsultationsSince(since: Long): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM consultations WHERE followUpDate IS NOT NULL AND followUpDate <= :now AND followUpDate > 0")
    suspend fun getDueFollowUps(now: Long = System.currentTimeMillis()): List<ConsultationEntity>

    @Query("SELECT * FROM consultations WHERE isFlagged = 1 ORDER BY timestamp DESC")
    fun getFlaggedConsultations(): Flow<List<ConsultationEntity>>

    @Query("SELECT COUNT(*) FROM consultations WHERE timestamp >= :since")
    suspend fun getConsultationCountSince(since: Long): Int
}

@Dao
interface SymptomEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomEntry(entry: SymptomEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptomEntries(entries: List<SymptomEntryEntity>)

    @Query("SELECT * FROM symptom_entries WHERE consultationId = :consultationId")
    suspend fun getEntriesForConsultation(consultationId: Long): List<SymptomEntryEntity>
}

@Dao
interface MeshSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacket(packet: MeshSyncPacketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackets(packets: List<MeshSyncPacketEntity>)

    @Query("SELECT * FROM mesh_sync_packets ORDER BY timestamp DESC")
    fun getAllPackets(): Flow<List<MeshSyncPacketEntity>>

    @Query("SELECT * FROM mesh_sync_packets WHERE gpsZone = :zone AND timestamp >= :since ORDER BY timestamp DESC")
    fun getPacketsInZone(zone: String, since: Long): Flow<List<MeshSyncPacketEntity>>

    @Query("SELECT * FROM mesh_sync_packets WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getRecentPackets(since: Long): List<MeshSyncPacketEntity>

    @Query("SELECT COUNT(DISTINCT sourceWorkerIdHash) FROM mesh_sync_packets")
    suspend fun getUniqueDeviceCount(): Int

    @Query("SELECT MAX(syncedAt) FROM mesh_sync_packets")
    suspend fun getLastSyncTime(): Long?
}

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertRecordEntity): Long

    @Query("SELECT * FROM alert_records WHERE isActive = 1 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<AlertRecordEntity>>

    @Query("UPDATE alert_records SET isActive = 0 WHERE id = :alertId")
    suspend fun deactivateAlert(alertId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertRecordEntity>)
}

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>)

    @Query("SELECT * FROM medicines ORDER BY genericName ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE category = :category ORDER BY genericName ASC")
    fun getMedicinesByCategory(category: String): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE genericName = :name LIMIT 1")
    suspend fun getMedicineByName(name: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE genericName LIKE '%' || :query || '%' OR localName LIKE '%' || :query || '%'")
    fun searchMedicines(query: String): Flow<List<MedicineEntity>>

    @Query("SELECT COUNT(*) FROM medicines")
    suspend fun getMedicineCount(): Int

    @Transaction
    @Query("SELECT * FROM medicines ORDER BY genericName ASC")
    fun getAllMedicinesWithInventory(): Flow<List<MedicineWithInventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: MedicineInventoryEntity)

    @Query("SELECT * FROM medicine_inventory WHERE medicineId = :medicineId")
    suspend fun getInventoryForMedicine(medicineId: Long): MedicineInventoryEntity?

    @Query("SELECT * FROM medicine_inventory")
    fun getAllInventory(): Flow<List<MedicineInventoryEntity>>
}

@Dao
interface ConditionMedicineMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<ConditionMedicineMappingEntity>)

    @Query("SELECT * FROM condition_medicine_mappings WHERE conditionName = :condition ORDER BY priority ASC")
    suspend fun getMedicinesForCondition(condition: String): List<ConditionMedicineMappingEntity>

    @Query("SELECT COUNT(*) FROM condition_medicine_mappings")
    suspend fun getMappingCount(): Int
}

@Dao
interface DoctorRegistryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorRegistryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctors(doctors: List<DoctorRegistryEntity>)

    @Query("SELECT * FROM doctor_registry ORDER BY phcName ASC")
    fun getAllDoctors(): Flow<List<DoctorRegistryEntity>>

    @Query("SELECT * FROM doctor_registry WHERE currentStatus = 'FREE' ORDER BY phcName ASC")
    suspend fun getFreeDoctors(): List<DoctorRegistryEntity>

    @Query("SELECT * FROM doctor_registry WHERE currentStatus = 'BUSY_LOW_RISK'")
    suspend fun getBusyLowRiskDoctors(): List<DoctorRegistryEntity>

    @Update
    suspend fun updateDoctor(doctor: DoctorRegistryEntity)

    @Query("SELECT COUNT(*) FROM doctor_registry")
    suspend fun getDoctorCount(): Int

    @Query("SELECT * FROM doctor_registry WHERE id = :doctorId")
    suspend fun getDoctorById(doctorId: Long): DoctorRegistryEntity?
}
