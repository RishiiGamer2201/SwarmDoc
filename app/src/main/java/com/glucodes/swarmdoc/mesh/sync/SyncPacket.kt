package com.glucodes.swarmdoc.mesh.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Anonymized symptom packet for mesh sync.
 * This is the core data structure exchanged between devices via BLE.
 * All data is anonymized - no patient names or identifying information.
 */
@Serializable
data class SyncPacket(
    val packetId: String,
    val workerIdHash: String,
    val symptomVector: List<String>,
    val conditionSuspected: String = "",
    val caseCount: Int = 1,
    val gpsZone: String,
    val timestamp: Long,
    val protocolVersion: Int = 1,
    val vectorClock: Long = timestamp // Simplistic clock for conflict resolution
) {
    /**
     * Serialize to JSON bytes for BLE transmission.
     */
    fun toBytes(): ByteArray {
        return Json.encodeToString(SyncPacket.serializer(), this).toByteArray()
    }

}

/**
 * Deserialize from JSON bytes received via BLE.
 */
fun parseSyncPacket(bytes: ByteArray): SyncPacket? {
    return try {
        Json.decodeFromString(SyncPacket.serializer(), String(bytes))
    } catch (e: Exception) {
        null
    }
}
