package com.glucodes.swarmdoc.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.glucodes.swarmdoc.ui.navigation.Screen
import com.glucodes.swarmdoc.ui.theme.*

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val tint: Color? = null,
)

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    privacyMode: Boolean = false,
) {
    val items = listOf(
        BottomNavItem("Home", Icons.Rounded.Home, Screen.Home.route),
        BottomNavItem("Patients", Icons.Rounded.People, Screen.PatientHistory.route),
        BottomNavItem("Community", Icons.Rounded.Hub, Screen.CommunityMesh.route),
        BottomNavItem("SOS", Icons.Rounded.NotificationsActive, Screen.Sos.route, tint = CoralRed),
        BottomNavItem("Profile", Icons.Rounded.Person, Screen.WorkerProfile.route),
    )

    val containerColor = if (privacyMode) PrivacySurface else White

    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 4.dp,
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val defaultTint = if (privacyMode) PrivacyIndigo else ForestGreen
            val iconTint = when {
                item.tint != null && isSelected -> item.tint
                item.tint != null -> item.tint.copy(alpha = 0.6f)
                isSelected -> defaultTint
                else -> WarmGrey
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = iconTint,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = if (privacyMode) PrivacyIndigoLight.copy(alpha = 0.2f) else TurmericGold.copy(alpha = 0.2f),
                ),
            )
        }
    }
}
