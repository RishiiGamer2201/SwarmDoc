package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.components.PatientCard
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.PatientViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistoryScreen(
    onNavigateToPatientDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: PatientViewModel = hiltViewModel()
) {
    val patients by viewModel.filteredPatients.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.filterRisk.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        // Top section with title and search
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestGreen)
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Patient History",
                        style = MaterialTheme.typography.displaySmall,
                        color = White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search by name...", color = White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = TurmericGold,
                        unfocusedBorderColor = White.copy(alpha = 0.3f),
                        cursorColor = TurmericGold,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == null,
                onClick = { viewModel.setFilterRisk(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TurmericGold)
            )
            FilterChip(
                selected = activeFilter == "EMERGENCY",
                onClick = { viewModel.setFilterRisk("EMERGENCY") },
                label = { Text("Emergency") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CoralRed, selectedLabelColor = White)
            )
            FilterChip(
                selected = activeFilter == "URGENT",
                onClick = { viewModel.setFilterRisk("URGENT") },
                label = { Text("Urgent") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WarmAmber)
            )
        }

        // List
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(patients, key = { it.patient.id }) { record ->
                val latestConsultation = record.consultations.maxByOrNull { it.timestamp }
                
                PatientCard(
                    initials = record.patient.initials,
                    name = record.patient.name,
                    topSymptom = if (latestConsultation?.isFlagged == true) "Flagged for follow-up" else "Record viewed",
                    riskLevel = latestConsultation?.riskLevel ?: "NORMAL",
                    timestamp = latestConsultation?.timestamp ?: record.patient.createdAt,
                    followUpDue = latestConsultation?.followUpDate != null,
                    compact = false,
                    onClick = { onNavigateToPatientDetail(record.patient.id) }
                )
            }
            if (patients.isEmpty()) {
                item {
                    Text(
                        "No patients found fitting this filter.",
                        color = WarmGrey,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
