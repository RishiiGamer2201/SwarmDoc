package com.glucodes.swarmdoc.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "swarmdoc_prefs")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Keys
    private val languageKey = stringPreferencesKey("language")
    private val workerNameKey = stringPreferencesKey("worker_name")
    private val workerPinKey = stringPreferencesKey("worker_pin")
    private val districtKey = stringPreferencesKey("district")
    private val subDistrictKey = stringPreferencesKey("sub_district")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val privacyModeKey = booleanPreferencesKey("privacy_mode")
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val focusAreaKey = stringPreferencesKey("focus_area")
    private val ashaIdKey = stringPreferencesKey("asha_id")
    private val workerPhotoPathKey = stringPreferencesKey("worker_photo_path")

    val language: Flow<String> = dataStore.data.map { it[languageKey] ?: "en" }
    val workerName: Flow<String> = dataStore.data.map { it[workerNameKey] ?: "ASHA Worker" }
    val isOnboarded: Flow<Boolean> = dataStore.data.map { it[onboardedKey] ?: false }
    val isOnboardingComplete: Flow<Boolean> = dataStore.data.map { it[onboardingCompleteKey] ?: false }
    val isPrivacyMode: Flow<Boolean> = dataStore.data.map { it[privacyModeKey] ?: false }
    val isDarkMode: Flow<Boolean> = dataStore.data.map { it[darkModeKey] ?: false }
    val district: Flow<String> = dataStore.data.map { it[districtKey] ?: "" }
    val subDistrict: Flow<String> = dataStore.data.map { it[subDistrictKey] ?: "" }
    val focusArea: Flow<String> = dataStore.data.map { it[focusAreaKey] ?: "General Health" }
    val ashaId: Flow<String> = dataStore.data.map { it[ashaIdKey] ?: "" }
    val workerPhotoPath: Flow<String> = dataStore.data.map { it[workerPhotoPathKey] ?: "" }

    suspend fun setLanguage(lang: String) {
        dataStore.edit { it[languageKey] = lang }
    }

    suspend fun setWorkerName(name: String) {
        dataStore.edit { it[workerNameKey] = name }
    }

    suspend fun setWorkerPin(pin: String) {
        dataStore.edit { it[workerPinKey] = pin }
    }

    suspend fun setDistrict(district: String) {
        dataStore.edit { it[districtKey] = district }
    }

    suspend fun setSubDistrict(subDistrict: String) {
        dataStore.edit { it[subDistrictKey] = subDistrict }
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        dataStore.edit { it[onboardedKey] = onboarded }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[onboardingCompleteKey] = complete }
    }

    suspend fun setPrivacyMode(enabled: Boolean) {
        dataStore.edit { it[privacyModeKey] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setFocusArea(area: String) {
        dataStore.edit { it[focusAreaKey] = area }
    }

    suspend fun setAshaId(id: String) {
        dataStore.edit { it[ashaIdKey] = id }
    }

    suspend fun setWorkerPhotoPath(path: String) {
        dataStore.edit { it[workerPhotoPathKey] = path }
    }

    suspend fun verifyPin(input: String): Boolean {
        val prefs = dataStore.data.first()
        val stored = prefs[workerPinKey] ?: ""
        return input == stored
    }
}
