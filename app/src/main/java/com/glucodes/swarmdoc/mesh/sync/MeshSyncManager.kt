package com.glucodes.swarmdoc.mesh.sync

import com.glucodes.swarmdoc.data.local.entities.MeshSyncPacketEntity
import com.glucodes.swarmdoc.data.repository.MeshRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// SyncPacket is declared in SyncPacket.kt

/**
 * Manages mesh sync operations including CRDT-style packet merge, cluster detection, and epidemic trend analysis.
 */
@Singleton
class MeshSyncManager @Inject constructor(
    private val meshRepository: MeshRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var isSyncing = false

    /**
     * Create a sync packet from a consultation's symptom data.
     */
    fun createPacket(
        workerIdHash: String,
        symptoms: List<String>,
        conditionSuspected: String,
        caseCount: Int,
        gpsZone: String,
    ): SyncPacket {
        val now = System.currentTimeMillis()
        return SyncPacket(
            packetId = "${workerIdHash}_$now",
            workerIdHash = workerIdHash,
            symptomVector = symptoms,
            conditionSuspected = conditionSuspected,
            caseCount = caseCount,
            gpsZone = gpsZone,
            timestamp = now,
            vectorClock = now // Using timestamp as simple LWW (Last-Writer-Wins) clock
        )
    }

    /**
     * Parse BLE mapped payload and merge using CRDT logic
     */
    suspend fun processIncomingPayload(payload: ByteArray) {
        try {
            val jsonString = String(payload)
            val packet = json.decodeFromString<SyncPacket>(jsonString)
            mergePackets(listOf(packet))
        } catch (e: Exception) {
            // Invalid payload, ignore
        }
    }

    /**
     * Merge received packets with local data using CRDT Last-Writer-Wins logic.
     * deduplicates based on packetId and vectorClock.
     */
    suspend fun mergePackets(incomingPackets: List<SyncPacket>) {
        val existingPackets = meshRepository.getRecentPackets(System.currentTimeMillis() - 30L * 86400000L)
        val existingMap = existingPackets.associateBy { it.sourceWorkerIdHash + "_" + it.timestamp } // Rough packetId recreation

        val newEntities = mutableListOf<MeshSyncPacketEntity>()

        for (packet in incomingPackets) {
            val existing = existingMap[packet.packetId]
            // CRDT Logic: If we don't have it, or incoming has a newer vector clock, we accept it
            if (existing == null || packet.vectorClock > existing.timestamp) { // using timestamp as proxy for vector clock in entity
                newEntities.add(
                    MeshSyncPacketEntity(
                        sourceWorkerIdHash = packet.workerIdHash,
                        anonymizedSymptomsJson = json.encodeToString(packet.symptomVector),
                        conditionSuspected = packet.conditionSuspected,
                        caseCount = packet.caseCount,
                        gpsZone = packet.gpsZone,
                        timestamp = packet.timestamp,
                        syncedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
        
        if (newEntities.isNotEmpty()) {
            meshRepository.insertPackets(newEntities)
        }
    }

    /**
     * Gets a 7-day rolling window of case counts grouped by condition.
     * Used for building Epidemic Timelines in UI.
     */
    suspend fun getEpidemiologicalTrend(): Map<String, List<Int>> {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7 * 86400000L
        val packets = meshRepository.getRecentPackets(oneWeekAgo)
        
        val trend = mutableMapOf<String, MutableList<Int>>()
        
        // Initialize an empty 7-day array for top conditions
        val topConditions = packets.map { it.conditionSuspected }.filter { it.isNotBlank() }.distinct()
        for (condition in topConditions) {
            trend[condition] = MutableList(7) { 0 }
        }
        
        for (packet in packets) {
            if (packet.conditionSuspected.isBlank()) continue
            
            // Calculate which day index (0 = today, 6 = 6 days ago)
            val daysAgo = ((now - packet.timestamp) / 86400000L).toInt().coerceIn(0, 6)
            trend[packet.conditionSuspected]?.let { list ->
                // Store chronological order: index 0 = 6 days ago, index 6 = today
                val chronologicalIndex = 6 - daysAgo
                list[chronologicalIndex] += packet.caseCount
            }
        }
        
        // Filter out empty trends and return
        return trend.filter { it.value.sum() > 0 }
    }

    /**
     * Detect symptom clusters in the mesh data.
     * Returns cluster info if a statistical threshold is crossed.
     */
    suspend fun detectClusters(gpsZone: String): List<ClusterInfo> {
        val oneWeekAgo = System.currentTimeMillis() - 7 * 86400000L
        val packets = meshRepository.getRecentPackets(oneWeekAgo)

        // Group by condition
        val conditionCounts = mutableMapOf<String, Int>()
        for (packet in packets) {
            if (packet.gpsZone == gpsZone && packet.conditionSuspected.isNotEmpty()) {
                conditionCounts[packet.conditionSuspected] =
                    (conditionCounts[packet.conditionSuspected] ?: 0) + packet.caseCount
            }
        }

        // Detect clusters (threshold: 3+ cases of same condition)
        return conditionCounts.filter { it.value >= 3 }.map { (condition, count) ->
            ClusterInfo(
                condition = condition,
                caseCount = count,
                gpsZone = gpsZone,
                isCritical = count >= 5,
                message = if (count >= 5) {
                    "$count cases of $condition detected in your area this week — possible outbreak. Escalate to PHC immediately."
                } else {
                    "$count cases of $condition detected in your area this week. Monitor closely."
                }
            )
        }.sortedByDescending { it.caseCount }
    }

    /**
     * Simulate a BLE sync for demo purposes if no peers are found.
     */
    suspend fun simulateSync(): SimulationResult {
        isSyncing = true
        val simulatedPackets = listOf(
            SyncPacket(
                packetId = "sim_${System.currentTimeMillis()}_1",
                workerIdHash = "w_hash_005",
                symptomVector = listOf("fever", "joint_pain", "rash"),
                conditionSuspected = "Dengue",
                caseCount = 1,
                gpsZone = "PURI_ZONE_A",
                timestamp = System.currentTimeMillis() - 3600000,
                vectorClock = 1L
            ),
            SyncPacket(
                packetId = "sim_${System.currentTimeMillis()}_2",
                workerIdHash = "w_hash_006",
                symptomVector = listOf("diarrhea", "vomiting", "fever"),
                conditionSuspected = "Gastroenteritis",
                caseCount = 2,
                gpsZone = "PURI_ZONE_A",
                timestamp = System.currentTimeMillis() - 7200000,
                vectorClock = 1L
            ),
        )
        mergePackets(simulatedPackets)
        isSyncing = false

        return SimulationResult(
            packetsReceived = simulatedPackets.size,
            newCases = simulatedPackets.sumOf { it.caseCount },
            devicesConnected = 2,
        )
    }

    fun isSyncing(): Boolean = isSyncing
}

data class ClusterInfo(
    val condition: String,
    val caseCount: Int,
    val gpsZone: String,
    val isCritical: Boolean,
    val message: String,
)

data class SimulationResult(
    val packetsReceived: Int,
    val newCases: Int,
    val devicesConnected: Int,
)
