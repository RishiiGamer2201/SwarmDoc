package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceNotesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val workerName by viewModel.workerName.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()
    val district by viewModel.district.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        // App Bar
        TopAppBar(
            title = { Text("Profile & Settings") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ForestGreen,
                titleContentColor = White
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp, 24.dp, 24.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Worker Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TurmericGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                workerName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalBlack
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = workerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalBlack
                            )
                            Text(
                                text = "ASHA Worker • $district",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmGrey
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalBlack
                )
            }

            // Settings Items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Rounded.Language,
                            title = "Language",
                            subtitle = "English",
                            onClick = { /* TODO */ }
                        )
                        Divider(color = ParchmentDark)
                        
                        SettingsRowToggle(
                            icon = Icons.Rounded.Security,
                            title = "Privacy Mode Default",
                            subtitle = "Start app in privacy mode",
                            checked = isPrivacyMode,
                            onCheckedChange = { viewModel.setPrivacyMode(it) }
                        )
                        Divider(color = ParchmentDark)
                        
                        SettingsRow(
                            icon = Icons.Rounded.Bluetooth,
                            title = "Bluetooth Mesh",
                            subtitle = "Enabled",
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalBlack
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Rounded.CloudUpload,
                            title = "Backup to Central Server",
                            subtitle = "Pending (WiFi required)",
                            iconTint = SageGreen,
                            onClick = { /* TODO */ }
                        )
                        Divider(color = ParchmentDark)
                        SettingsRow(
                            icon = Icons.Rounded.DeleteOutline,
                            title = "Clear Local Data",
                            subtitle = "Free up device storage",
                            iconTint = CoralRed,
                            onClick = { /* TODO */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = ForestGreen,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CharcoalBlack)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = LightGrey)
    }
}

@Composable
fun SettingsRowToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ForestGreen)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CharcoalBlack)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = White,
                checkedTrackColor = PrivacyLavender,
            )
        )
    }
}
