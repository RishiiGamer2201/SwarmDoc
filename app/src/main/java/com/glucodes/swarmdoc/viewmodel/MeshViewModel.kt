package com.glucodes.swarmdoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glucodes.swarmdoc.data.repository.MeshRepository
import com.glucodes.swarmdoc.mesh.bluetooth.BleAdvertiser
import com.glucodes.swarmdoc.mesh.bluetooth.BleScanner
import com.glucodes.swarmdoc.mesh.sync.ClusterInfo
import com.glucodes.swarmdoc.mesh.sync.MeshSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeshViewModel @Inject constructor(
    private val meshRepository: MeshRepository,
    private val meshSyncManager: MeshSyncManager,
    private val bleAdvertiser: BleAdvertiser,
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _clusters = MutableStateFlow<List<ClusterInfo>>(emptyList())
    val clusters: StateFlow<List<ClusterInfo>> = _clusters.asStateFlow()
    
    // Discovered devices via BLE
    private val _discoveredDevices = MutableStateFlow<List<BleScanner.DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleScanner.DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    val uniqueDeviceCount: StateFlow<Int> = flow {
        while (true) {
            emit(meshRepository.getUniqueDeviceCount())
            delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastSyncTime: StateFlow<Long?> = flow {
        while (true) {
            emit(meshRepository.getLastSyncTime())
            delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    // 7-day Epidemic Trend (Simple Map of Condition -> List of daily counts)
    private val _epidemicTrend = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val epidemicTrend: StateFlow<Map<String, List<Int>>> = _epidemicTrend.asStateFlow()

    init {
        loadClusters()
        loadEpidemicTrend()
    }

    private fun loadClusters() {
        viewModelScope.launch {
            _clusters.value = meshSyncManager.detectClusters("PURI_ZONE_A") 
        }
    }
    
    private fun loadEpidemicTrend() {
        viewModelScope.launch {
            _epidemicTrend.value = meshSyncManager.getEpidemiologicalTrend()
        }
    }

    fun startBleScan() {
        bleScanner.initialize()
        bleScanner.startScanning()
        
        // Polling loop for UI updates
        viewModelScope.launch {
            while (bleScanner.isScanning()) {
                _discoveredDevices.value = bleScanner.getDiscoveredDevices()
                delay(1000)
            }
        }
    }
    
    fun stopBleScan() {
        bleScanner.stopScanning()
    }

    fun triggerRealSync() {
        if (_isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            
            // In a real app we'd connect via BLE, exchange vector clocks, and sync packets
            // We simulate the time delay of BLE transmission here, then merge locally
            delay(2000)
            
            if (_discoveredDevices.value.isNotEmpty()) {
                // Read payloads from discovered devices and merge into CRDT
                for (device in _discoveredDevices.value) {
                    val payload = device.serviceData
                    if (payload != null) {
                        meshSyncManager.processIncomingPayload(payload)
                    }
                }
            } else {
                // Fallback to purely simulated sync if no real BLE devices nearby
                meshSyncManager.simulateSync()
            }
            
            loadClusters()
            loadEpidemicTrend()
            
            _isSyncing.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopBleScan()
        bleAdvertiser.stopAdvertising()
    }
}
