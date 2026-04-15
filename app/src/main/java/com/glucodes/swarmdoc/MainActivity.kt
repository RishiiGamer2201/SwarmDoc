package com.glucodes.swarmdoc

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.glucodes.swarmdoc.data.local.DatabaseSeeder
import com.glucodes.swarmdoc.data.repository.SettingsRepository
import com.glucodes.swarmdoc.ui.navigation.Screen
import com.glucodes.swarmdoc.ui.navigation.SwarmDocNavGraph
import com.glucodes.swarmdoc.ui.theme.SwarmDocTheme
import com.glucodes.swarmdoc.util.ProvideLocalizedStrings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    // Call this from wherever the user selects their language in the app
    fun changeLanguage(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed database on first launch
        lifecycleScope.launch {
            databaseSeeder.seedIfNeeded()
        }

        setContent {
            var privacyMode by remember { mutableStateOf(false) }

            // Permission Launcher for Mic, Camera, and Location
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                // The map and camera will automatically work once these are granted
            }

            // Fire off the permission request when the app loads
            LaunchedEffect(Unit) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            // Observe privacy mode from settings
            LaunchedEffect(Unit) {
                settingsRepository.isPrivacyMode.collect { privacyMode = it }
            }

            // Observe language from settings
            var currentLanguage by remember { mutableStateOf("en") }
            LaunchedEffect(Unit) {
                settingsRepository.language.collect { currentLanguage = it }
            }

            SwarmDocTheme(privacyMode = privacyMode) {
                ProvideLocalizedStrings(languageCode = currentLanguage) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()

                        SwarmDocNavGraph(
                            navController = navController,
                            startDestination = Screen.Splash.route,
                            privacyMode = privacyMode,
                            onPrivacyModeChange = { enabled ->
                                privacyMode = enabled
                            },
                        )
                    }
                }
            }
        }
    }
}