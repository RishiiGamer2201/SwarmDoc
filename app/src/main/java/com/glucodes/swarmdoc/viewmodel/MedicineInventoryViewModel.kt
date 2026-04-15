package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.local.entities.MedicineWithInventory
import com.glucodes.swarmdoc.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicineInventoryViewModel @Inject constructor(
    private val medicineRepository: MedicineRepository,
) : ViewModel() {

    val medicines: StateFlow<List<MedicineWithInventory>> =
        medicineRepository.getAllMedicinesWithInventory()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleStock(medicineId: Long, inStock: Boolean) {
        viewModelScope.launch {
            medicineRepository.updateInventory(medicineId, inStock)
        }
    }

    fun updateDetails(
        medicineId: Long,
        quantity: Int,
        unitPrice: Double,
        mfdDate: String,
        expiryDate: String,
    ) {
        viewModelScope.launch {
            medicineRepository.updateInventoryDetails(
                medicineId = medicineId,
                quantity = quantity,
                unitPrice = unitPrice,
                mfdDate = mfdDate,
                expiryDate = expiryDate,
            )
        }
    }
}
