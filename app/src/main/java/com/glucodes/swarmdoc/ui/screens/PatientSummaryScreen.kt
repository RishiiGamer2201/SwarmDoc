package com.glucodes.swarmdoc.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.domain.models.Diagnosis
import com.glucodes.swarmdoc.ui.components.RiskBadgeSmall
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.QrCodeGenerator
import com.glucodes.swarmdoc.viewmodel.PatientViewModel
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientSummaryScreen(
    consultationId: Long,
    privacyMode: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val consultation by viewModel.selectedConsultation.collectAsState()
    val patientData by viewModel.selectedPatient.collectAsState()
    val json = remember { Json { ignoreUnknownKeys = true } }

    LaunchedEffect(consultationId) {
        viewModel.loadConsultation(consultationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clinical Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.Home, contentDescription = "Home")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (privacyMode) PrivacySurface else ForestGreen,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                )
            )
        },
        containerColor = if (privacyMode) PrivacySurface else Parchment
    ) { padding ->
        if (consultation != null && patientData != null) {
            val diag: Diagnosis? = try {
                json.decodeFromString(consultation!!.diagnosisJson)
            } catch (e: Exception) { null }

            val symptoms: List<String> = try {
                json.decodeFromString(consultation!!.symptomsJson)
            } catch (e: Exception) { emptyList() }

            val qrBitmap = remember(consultationId) {
                val dataStr = if (privacyMode) "HASH_12345" else "ID:${patientData!!.patient.id}|NAM:${patientData!!.patient.name}"
                QrCodeGenerator.generateQrCode(dataStr, 256)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Official looking card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "SwarmDoc Medical Record",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarmGrey
                            )
                            Text(
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(consultation!!.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGrey
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (privacyMode) "Patient ${patientData!!.patient.initials}" else patientData!!.patient.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalBlack
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${patientData!!.patient.age} yrs • " + 
                                           (if (privacyMode) "F" else patientData!!.patient.sex) + " • " + 
                                           (if (privacyMode) "Zone A" else patientData!!.patient.village),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CharcoalBlack
                                )
                            }
                            
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Diagnosis
                        val primaryCondition = diag?.conditions?.firstOrNull()?.nameEnglish ?: "Awaiting Triage"
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Assessment",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGrey,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                text = if (privacyMode) "Category Protocol" else primaryCondition,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalBlack
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Risk Level",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGrey,
                                modifier = Modifier.width(90.dp)
                            )
                            RiskBadgeSmall(riskLevel = consultation!!.riskLevel)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = "Symptoms",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGrey,
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                text = if (privacyMode) "Restricted List" else symptoms.joinToString(", ") { it.replace("_", " ").replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString() } },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalBlack
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Guidelines based on National Health Mission protocol. This is a triage output, not a definitive diagnosis.",
                            style = MaterialTheme.typography.labelSmall,
                            color = LightGrey,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Medical Note: Patient ${patientData!!.patient.initials}. Assessment: Case implies ${diag?.conditions?.firstOrNull()?.nameEnglish}. Urgency: ${consultation!!.riskLevel}. Require consultation.")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Patient Record"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SageGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WhatsApp", color = White)
                    }

                    OutlinedButton(
                        onClick = { /* PDF Generation stub */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save PDF")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
