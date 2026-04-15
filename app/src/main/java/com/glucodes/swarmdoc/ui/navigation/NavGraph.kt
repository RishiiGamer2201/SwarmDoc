package com.glucodes.swarmdoc.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glucodes.swarmdoc.ui.components.BottomNavBar
import com.glucodes.swarmdoc.ui.screens.*

@Composable
fun SwarmDocNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    privacyMode: Boolean = false,
    onPrivacyModeChange: (Boolean) -> Unit = {},
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val canGoBack = navController.previousBackStackEntry != null

    // Bottom bar visible only on main 4 tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.PatientHistory.route,
        Screen.CommunityMesh.route,
        Screen.Sos.route,
        Screen.WorkerProfile.route,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: "",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    privacyMode = privacyMode,
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
                popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { it / 4 },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
            // Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLanguageSelection = {
                        navController.navigate(Screen.LanguageSelection.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Language Selection (appears first, before onboarding)
            composable(Screen.LanguageSelection.route) {
                LanguageSelectionScreen(
                    onLanguageSelected = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route
                        if (previousRoute == Screen.WorkerProfile.route) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.OnboardingWalkthrough.route) {
                                popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Onboarding Walkthrough (Change 1)
            composable(Screen.OnboardingWalkthrough.route) {
                OnboardingWalkthroughScreen(
                    onComplete = {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.OnboardingWalkthrough.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.LanguageSelection.route) {
                            popUpTo(Screen.OnboardingWalkthrough.route) { inclusive = true }
                        }
                    }
                )
            }

            // Profile Setup (post-onboarding)
            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Home (Change 3)
            composable(Screen.Home.route) {
                HomeScreen(
                    privacyMode = privacyMode,
                    onNavigateToConsultation = { navController.navigate(Screen.ConsultationFlow.route) },
                    onNavigateToPatients = {
                        navController.navigate(Screen.PatientHistory.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCommunity = {
                        navController.navigate(Screen.CommunityMesh.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToInventory = { navController.navigate(Screen.MedicineInventory.route) },
                    onNavigateToDiagnostics = { navController.navigate(Screen.SpecialDiagnostics.route) },
                    onPrivacyModeChange = onPrivacyModeChange,
                )
            }

            // Consultation Flow (Changes 4, 8, 10)
            composable(Screen.ConsultationFlow.route) {
                ConsultationFlowScreen(
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToSos = {
                        navController.navigate(Screen.Sos.route)
                    },
                )
            }

            // Patient History — now with back navigation
            composable(Screen.PatientHistory.route) {
                PatientHistoryScreen(
                    onNavigateToPatientDetail = { patientId ->
                        navController.navigate(Screen.PatientDetail.createRoute(patientId))
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Patient Detail
            composable(
                route = Screen.PatientDetail.route,
                arguments = listOf(navArgument("patientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getLong("patientId") ?: 0L
                PatientDetailScreen(
                    patientId = patientId,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Community Mesh — now with back navigation
            composable(Screen.CommunityMesh.route) {
                CommunityMeshScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // SOS — now with back navigation
            composable(Screen.Sos.route) {
                SosScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Worker Profile (Change 2)
            composable(Screen.WorkerProfile.route) {
                WorkerProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToInventory = { navController.navigate(Screen.MedicineInventory.route) },
                    onNavigateToLanguage = { navController.navigate(Screen.LanguageSelection.route) },
                )
            }

            // Medicine Inventory (Change 6)
            composable(Screen.MedicineInventory.route) {
                MedicineInventoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Diagnostics Routes
            composable(Screen.SpecialDiagnostics.route) {
                SpecialDiagnosticsScreen(
                    onNavigateToCoughScreener = { navController.navigate(Screen.CoughScreener.route) },
                    onNavigateToAnemiaCheck = { navController.navigate(Screen.AnemiaCheck.route) },
                    onNavigateToRppg = { navController.navigate(Screen.RppgVitals.route) },
                    onNavigateToCapillaryRefill = { navController.navigate(Screen.CapillaryRefill.route) },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            
            composable(Screen.CoughScreener.route) {
                CoughScreenerScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable(Screen.AnemiaCheck.route) {
                AnemiaEyeCheckScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable(Screen.RppgVitals.route) {
                RppgHeartRateScreen(onNavigateBack = { navController.popBackStack() })
            }
            
            composable(Screen.CapillaryRefill.route) {
                CapillaryRefillScreen(onNavigateBack = { navController.popBackStack() })
            }
            }

            // Global back button so every screen has a top-left back affordance.
            if (canGoBack && currentRoute != Screen.Splash.route) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }
    }
}
