package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.Constants
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val strings = LocalStrings.current
    val workerName by viewModel.workerName.collectAsState()
    val district by viewModel.district.collectAsState()
    val focusArea by viewModel.focusArea.collectAsState()
    val ashaId by viewModel.ashaId.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        TopAppBar(
            title = { Text(strings.myProfile, fontWeight = FontWeight.Bold, color = ForestGreen) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            // Avatar and name header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(TurmericGold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(workerName.take(1).uppercase(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(workerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                    Text("${strings.district}: $district", style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
                    if (ashaId.isNotBlank()) Text("ASHA ID: $ashaId", style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Focus area selector (Change 5)
            Text(strings.myPrimaryFocus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
            Spacer(modifier = Modifier.height(8.dp))
            Constants.FOCUS_AREAS.forEach { area ->
                val selected = focusArea == area
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.setFocusArea(area) },
                    colors = CardDefaults.cardColors(containerColor = if (selected) ForestGreen.copy(alpha = 0.1f) else White),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(area, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = CharcoalBlack)
                        if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Medicine Inventory link
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToInventory() },
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.myCentresStock, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                        Text(strings.updateMedicineAvailability, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WarmGrey)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Export records
            Card(
                modifier = Modifier.fillMaxWidth().clickable { /* export JSON */ },
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.exportAllRecords, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                        Text(strings.downloadAsJson, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WarmGrey)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Language Setup
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToLanguage() },
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.changeLanguage, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                        Text(strings.chooseYourLanguage, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WarmGrey)
                }
            }
        }
    }
}
