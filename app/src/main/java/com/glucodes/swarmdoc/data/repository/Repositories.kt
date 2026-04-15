package com.glucodes.swarmdoc.data.repository

import com.glucodes.swarmdoc.data.local.dao.*
import com.glucodes.swarmdoc.data.local.entities.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val patientDao: PatientDao,
    private val consultationDao: ConsultationDao,
    private val symptomEntryDao: SymptomEntryDao,
) {
    fun getAllPatients(): Flow<List<PatientEntity>> = patientDao.getAllPatients()

    fun getAllPatientsWithConsultations(): Flow<List<PatientWithConsultations>> =
        patientDao.getAllPatientsWithConsultations()

    fun searchPatients(query: String): Flow<List<PatientEntity>> =
        patientDao.searchPatients(query)

    suspend fun getPatientById(id: Long): PatientEntity? = patientDao.getPatientById(id)

    suspend fun getPatientWithConsultations(id: Long): PatientWithConsultations? =
        patientDao.getPatientWithConsultations(id)

    suspend fun insertPatient(patient: PatientEntity): Long =
        patientDao.insertPatient(patient)

    suspend fun insertConsultation(consultation: ConsultationEntity): Long =
        consultationDao.insertConsultation(consultation)

    suspend fun updateConsultation(consultation: ConsultationEntity) =
        consultationDao.updateConsultation(consultation)

    suspend fun getConsultationById(id: Long): ConsultationEntity? =
        consultationDao.getConsultationById(id)

    fun getRecentConsultations(limit: Int = 10): Flow<List<ConsultationEntity>> =
        consultationDao.getRecentConsultations(limit)

    fun getConsultationsForPatient(patientId: Long): Flow<List<ConsultationEntity>> =
        consultationDao.getConsultationsForPatient(patientId)

    fun getConsultationsByRiskLevel(riskLevel: String): Flow<List<ConsultationEntity>> =
        consultationDao.getConsultationsByRiskLevel(riskLevel)

    fun getConsultationsSince(since: Long): Flow<List<ConsultationEntity>> =
        consultationDao.getConsultationsSince(since)

    fun getFlaggedConsultations(): Flow<List<ConsultationEntity>> =
        consultationDao.getFlaggedConsultations()

    suspend fun getDueFollowUps(): List<ConsultationEntity> =
        consultationDao.getDueFollowUps()

    suspend fun insertSymptomEntries(entries: List<SymptomEntryEntity>) =
        symptomEntryDao.insertSymptomEntries(entries)

    suspend fun getSymptomEntries(consultationId: Long): List<SymptomEntryEntity> =
        symptomEntryDao.getEntriesForConsultation(consultationId)

    suspend fun findPatientByNameAndDistrict(name: String, district: String): PatientEntity? =
        patientDao.findPatientByNameAndDistrict(name, district)

    suspend fun getPatientCount(): Int = patientDao.getPatientCount()

    suspend fun getConsultationCountSince(since: Long): Int =
        consultationDao.getConsultationCountSince(since)
}

@Singleton
class MeshRepository @Inject constructor(
    private val meshSyncDao: MeshSyncDao,
    private val alertDao: AlertDao,
) {
    fun getAllPackets(): Flow<List<MeshSyncPacketEntity>> = meshSyncDao.getAllPackets()

    fun getPacketsInZone(zone: String, since: Long): Flow<List<MeshSyncPacketEntity>> =
        meshSyncDao.getPacketsInZone(zone, since)

    suspend fun getRecentPackets(since: Long): List<MeshSyncPacketEntity> =
        meshSyncDao.getRecentPackets(since)

    suspend fun insertPacket(packet: MeshSyncPacketEntity): Long =
        meshSyncDao.insertPacket(packet)

    suspend fun insertPackets(packets: List<MeshSyncPacketEntity>) =
        meshSyncDao.insertPackets(packets)

    suspend fun getUniqueDeviceCount(): Int = meshSyncDao.getUniqueDeviceCount()

    suspend fun getLastSyncTime(): Long? = meshSyncDao.getLastSyncTime()

    fun getActiveAlerts(): Flow<List<AlertRecordEntity>> = alertDao.getActiveAlerts()

    suspend fun insertAlert(alert: AlertRecordEntity): Long = alertDao.insertAlert(alert)
}

@Singleton
class MedicineRepository @Inject constructor(
    private val medicineDao: MedicineDao,
    private val conditionMappingDao: ConditionMedicineMappingDao,
) {
    fun getAllMedicines(): Flow<List<MedicineEntity>> = medicineDao.getAllMedicines()

    fun getAllMedicinesWithInventory(): Flow<List<MedicineWithInventory>> =
        medicineDao.getAllMedicinesWithInventory()

    fun searchMedicines(query: String): Flow<List<MedicineEntity>> =
        medicineDao.searchMedicines(query)

    suspend fun getMedicineByName(name: String): MedicineEntity? =
        medicineDao.getMedicineByName(name)

    suspend fun updateInventory(medicineId: Long, inStock: Boolean) {
        val existing = medicineDao.getInventoryForMedicine(medicineId)
        medicineDao.insertInventory(
            MedicineInventoryEntity(
                medicineId = medicineId,
                inStock = inStock,
                quantity = existing?.quantity ?: 0,
                unitPrice = existing?.unitPrice ?: 0.0,
                mfdDate = existing?.mfdDate ?: "",
                expiryDate = existing?.expiryDate ?: "",
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateInventoryDetails(
        medicineId: Long,
        quantity: Int,
        unitPrice: Double,
        mfdDate: String,
        expiryDate: String,
    ) {
        val existing = medicineDao.getInventoryForMedicine(medicineId)
        medicineDao.insertInventory(
            MedicineInventoryEntity(
                medicineId = medicineId,
                inStock = existing?.inStock ?: true,
                quantity = quantity.coerceAtLeast(0),
                unitPrice = unitPrice.coerceAtLeast(0.0),
                mfdDate = mfdDate.trim(),
                expiryDate = expiryDate.trim(),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun getInventoryForMedicine(medicineId: Long): MedicineInventoryEntity? =
        medicineDao.getInventoryForMedicine(medicineId)

    suspend fun getMedicinesForCondition(condition: String): List<ConditionMedicineMappingEntity> =
        conditionMappingDao.getMedicinesForCondition(condition)

    suspend fun getMedicineCount(): Int = medicineDao.getMedicineCount()
    suspend fun getMappingCount(): Int = conditionMappingDao.getMappingCount()

    suspend fun insertMedicines(medicines: List<MedicineEntity>) =
        medicineDao.insertMedicines(medicines)

    suspend fun insertMappings(mappings: List<ConditionMedicineMappingEntity>) =
        conditionMappingDao.insertMappings(mappings)
}

@Singleton
class DoctorRepository @Inject constructor(
    private val doctorRegistryDao: DoctorRegistryDao,
) {
    fun getAllDoctors(): Flow<List<DoctorRegistryEntity>> = doctorRegistryDao.getAllDoctors()

    suspend fun getFreeDoctors(): List<DoctorRegistryEntity> = doctorRegistryDao.getFreeDoctors()

    suspend fun getBusyLowRiskDoctors(): List<DoctorRegistryEntity> =
        doctorRegistryDao.getBusyLowRiskDoctors()

    suspend fun updateDoctor(doctor: DoctorRegistryEntity) =
        doctorRegistryDao.updateDoctor(doctor)

    suspend fun getDoctorCount(): Int = doctorRegistryDao.getDoctorCount()

    suspend fun getDoctorById(id: Long): DoctorRegistryEntity? =
        doctorRegistryDao.getDoctorById(id)

    suspend fun insertDoctors(doctors: List<DoctorRegistryEntity>) =
        doctorRegistryDao.insertDoctors(doctors)
}
