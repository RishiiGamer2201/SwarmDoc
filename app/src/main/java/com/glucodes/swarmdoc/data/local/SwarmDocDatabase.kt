package com.glucodes.swarmdoc.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glucodes.swarmdoc.data.local.dao.*
import com.glucodes.swarmdoc.data.local.entities.*

@Database(
    entities = [
        PatientEntity::class,
        ConsultationEntity::class,
        SymptomEntryEntity::class,
        MeshSyncPacketEntity::class,
        AlertRecordEntity::class,
        MedicineEntity::class,
        MedicineInventoryEntity::class,
        ConditionMedicineMappingEntity::class,
        DoctorRegistryEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SwarmDocDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun symptomEntryDao(): SymptomEntryDao
    abstract fun meshSyncDao(): MeshSyncDao
    abstract fun alertDao(): AlertDao
    abstract fun medicineDao(): MedicineDao
    abstract fun conditionMedicineMappingDao(): ConditionMedicineMappingDao
    abstract fun doctorRegistryDao(): DoctorRegistryDao

    companion object {
        const val DATABASE_NAME = "swarmdoc_db"
    }
}
