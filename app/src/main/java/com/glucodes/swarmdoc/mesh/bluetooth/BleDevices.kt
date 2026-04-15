package com.glucodes.swarmdoc.mesh.bluetooth

import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE Advertiser for broadcasting anonymized symptom data.
 * Uses BluetoothLeAdvertiser to broadcast service data.
 */
@Singleton
class BleAdvertiser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    }

    private var advertiser: BluetoothLeAdvertiser? = null
    private var isAdvertising = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
        }
    }

    fun initialize(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return false
        advertiser = adapter.bluetoothLeAdvertiser
        return advertiser != null
    }

    /**
     * Start advertising with the given service data (serialized SyncPacket).
     */
    fun startAdvertising(serviceData: ByteArray): Boolean {
        if (advertiser == null || isAdvertising) return false

        try {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .addServiceData(ParcelUuid(SERVICE_UUID), serviceData.take(20).toByteArray())
                .build()

            advertiser?.startAdvertising(settings, data, advertiseCallback)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun stopAdvertising() {
        try {
            if (isAdvertising) {
                advertiser?.stopAdvertising(advertiseCallback)
                isAdvertising = false
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun isAdvertising(): Boolean = isAdvertising
}

/**
 * BLE Scanner for discovering nearby SwarmDoc devices.
 * Uses BluetoothLeScanner to find devices advertising our service UUID.
 */
@Singleton
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var scanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val discoveredDevices = mutableListOf<DiscoveredDevice>()

    data class DiscoveredDevice(
        val address: String,
        val serviceData: ByteArray?,
        val rssi: Int,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val serviceData = it.scanRecord?.getServiceData(ParcelUuid(BleAdvertiser.SERVICE_UUID))
                if (serviceData != null) {
                    discoveredDevices.add(
                        DiscoveredDevice(
                            address = it.device?.address ?: "unknown",
                            serviceData = serviceData,
                            rssi = it.rssi,
                        )
                    )
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
        }
    }

    fun initialize(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return false
        scanner = adapter.bluetoothLeScanner
        return scanner != null
    }

    /**
     * Start scanning for nearby SwarmDoc devices.
     */
    fun startScanning(): Boolean {
        if (scanner == null || isScanning) return false

        try {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleAdvertiser.SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            scanner?.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun stopScanning() {
        try {
            if (isScanning) {
                scanner?.stopScan(scanCallback)
                isScanning = false
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun getDiscoveredDevices(): List<DiscoveredDevice> = discoveredDevices.toList()

    fun clearDiscoveredDevices() {
        discoveredDevices.clear()
    }

    fun isScanning(): Boolean = isScanning
}
