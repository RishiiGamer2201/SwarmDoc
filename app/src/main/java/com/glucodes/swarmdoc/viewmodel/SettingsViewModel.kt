package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val language: StateFlow<String> = repository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val workerName: StateFlow<String> = repository.workerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isOnboarded: StateFlow<Boolean> = repository.isOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isOnboardingComplete: StateFlow<Boolean> = repository.isOnboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPrivacyMode: StateFlow<Boolean> = repository.isPrivacyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val district: StateFlow<String> = repository.district
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val subDistrict: StateFlow<String> = repository.subDistrict
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val focusArea: StateFlow<String> = repository.focusArea
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "General Health")

    val ashaId: StateFlow<String> = repository.ashaId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setLanguage(lang: String) {
        viewModelScope.launch { repository.setLanguage(lang) }
    }

    fun setWorkerName(name: String) {
        viewModelScope.launch { repository.setWorkerName(name) }
    }

    fun setWorkerPin(pin: String) {
        viewModelScope.launch { repository.setWorkerPin(pin) }
    }

    fun setDistrict(district: String) {
        viewModelScope.launch { repository.setDistrict(district) }
    }

    fun setSubDistrict(subDistrict: String) {
        viewModelScope.launch { repository.setSubDistrict(subDistrict) }
    }

    fun setOnboarded(onboarded: Boolean) {
        viewModelScope.launch { repository.setOnboarded(onboarded) }
    }

    fun setPrivacyMode(enabled: Boolean) {
        viewModelScope.launch { repository.setPrivacyMode(enabled) }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun setFocusArea(area: String) {
        viewModelScope.launch { repository.setFocusArea(area) }
    }

    fun completeSetup(name: String, district: String, pin: String) {
        viewModelScope.launch {
            repository.setWorkerName(name)
            repository.setDistrict(district)
            repository.setWorkerPin(pin)
            repository.setOnboardingComplete(true)
            repository.setOnboarded(true)
        }
    }
}
