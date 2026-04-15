package com.glucodes.swarmdoc.di

import android.content.Context
import androidx.room.Room
import com.glucodes.swarmdoc.data.local.SwarmDocDatabase
import com.glucodes.swarmdoc.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SwarmDocDatabase {
        return Room.databaseBuilder(
            context,
            SwarmDocDatabase::class.java,
            SwarmDocDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides fun providePatientDao(db: SwarmDocDatabase): PatientDao = db.patientDao()
    @Provides fun provideConsultationDao(db: SwarmDocDatabase): ConsultationDao = db.consultationDao()
    @Provides fun provideSymptomEntryDao(db: SwarmDocDatabase): SymptomEntryDao = db.symptomEntryDao()
    @Provides fun provideMeshSyncDao(db: SwarmDocDatabase): MeshSyncDao = db.meshSyncDao()
    @Provides fun provideAlertDao(db: SwarmDocDatabase): AlertDao = db.alertDao()
    @Provides fun provideMedicineDao(db: SwarmDocDatabase): MedicineDao = db.medicineDao()
    @Provides fun provideConditionMedicineMappingDao(db: SwarmDocDatabase): ConditionMedicineMappingDao = db.conditionMedicineMappingDao()
    @Provides fun provideDoctorRegistryDao(db: SwarmDocDatabase): DoctorRegistryDao = db.doctorRegistryDao()
}
