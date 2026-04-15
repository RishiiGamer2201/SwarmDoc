package com.glucodes.swarmdoc

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.glucodes.swarmdoc.util.Constants
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SwarmDocApplication : Application() {

    @Inject
    lateinit var databaseSeeder: com.glucodes.swarmdoc.data.local.DatabaseSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        seedDatabase()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val followUpChannel = NotificationChannel(
                Constants.CHANNEL_FOLLOWUP,
                "Follow-up Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders for patient follow-up visits"
            }

            val alertChannel = NotificationChannel(
                Constants.CHANNEL_ALERT,
                "Community Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Urgent community health alerts and outbreak notifications"
                enableVibration(true)
            }

            val meshChannel = NotificationChannel(
                Constants.CHANNEL_MESH,
                "Mesh Sync",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notifications about mesh network sync availability"
            }

            manager.createNotificationChannels(listOf(followUpChannel, alertChannel, meshChannel))
        }
    }

    private fun seedDatabase() {
        applicationScope.launch {
            try {
                databaseSeeder.seedIfNeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
