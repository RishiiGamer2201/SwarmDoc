package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.components.RiskBadgeSmall
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.PatientViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val patientData by viewModel.selectedPatient.collectAsState()

    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(patientData?.patient?.name ?: "Patient Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestGreen,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                )
            )
        },
        containerColor = Parchment
    ) { padding ->
        if (patientData != null) {
            val record = patientData!!
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // Patient Details Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Village: ${record.patient.village}", style = MaterialTheme.typography.bodyLarge)
                            Text("Age: ${record.patient.age}  |  Sex: ${record.patient.sex}", style = MaterialTheme.typography.bodyLarge)
                            Text("Registered: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(record.patient.createdAt))}", style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
                        }
                    }
                }
                
                item {
                    Text(
                        "Visit Timeline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalBlack,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(record.consultations.sortedByDescending { it.timestamp }) { consultation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            // Timeline node
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(ForestGreen)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(40.dp)
                                        .background(LightGrey)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                    Text(
                                        SimpleDateFormat("dd MMM yy, h:mm a", Locale.getDefault()).format(Date(consultation.timestamp)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarmGrey
                                    )
                                    RiskBadgeSmall(riskLevel = consultation.riskLevel)
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Input Mode: ${consultation.inputMode}", style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
