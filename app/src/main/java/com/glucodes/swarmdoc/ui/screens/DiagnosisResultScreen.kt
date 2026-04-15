package com.glucodes.swarmdoc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.domain.models.ConditionResult
import com.glucodes.swarmdoc.domain.models.RiskLevel
import com.glucodes.swarmdoc.ui.components.MeshNetworkAnimation
import com.glucodes.swarmdoc.ui.components.RiskBadge
import com.glucodes.swarmdoc.ui.components.ShimmerCard
import com.glucodes.swarmdoc.ui.components.ShimmerEffect
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.DiagnosisUiState
import com.glucodes.swarmdoc.viewmodel.DiagnosisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisResultScreen(
    consultationId: Long,
    privacyMode: Boolean,
    onNavigateToSummary: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DiagnosisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Medical Guidance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (privacyMode) PrivacySurface else Parchment,
                    titleContentColor = if (privacyMode) PrivacyIndigo else CharcoalBlack,
                )
            )
        },
        containerColor = if (privacyMode) PrivacySurface else Parchment
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            when (val state = uiState) {
                is DiagnosisUiState.Loading -> {
                    ShimmerEffect(height = 80.dp)
                    ShimmerCard(lines = 2)
                    ShimmerCard(lines = 3)
                    ShimmerEffect(height = 120.dp)
                }

                is DiagnosisUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.message,
                            color = CoralRed,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is DiagnosisUiState.Success -> {
                    val diagnosis = state.diagnosis
                    
                    // 1. Header Risk Badge
                    RiskBadge(riskLevel = diagnosis.riskLevel.name)

                    // 2. Possible Conditions
                    Text(
                        text = if (privacyMode) "Detected Findings" else "Possible Conditions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalBlack
                    )

                    diagnosis.conditions.forEachIndexed { index, condition ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { 50 },
                                animationSpec = tween(400, delayMillis = index * 100)
                            ) + fadeIn()
                        ) {
                            ConditionCard(condition, diagnosis.riskLevel)
                        }
                    }

                    // 3. Community Context Card (SwarmDoc Differentiator)
                    if (!privacyMode && diagnosis.communityContext != null) {
                        val ctx = diagnosis.communityContext
                        val isCluster = ctx.clusterDetected
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCluster) CoralRed.copy(alpha = 0.85f) else ForestGreen
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Network Animation background
                                MeshNetworkAnimation(
                                    modifier = Modifier.fillMaxSize(),
                                    nodeColor = White.copy(alpha = 0.4f),
                                    lineColor = White.copy(alpha = 0.2f),
                                    activeNodes = isCluster
                                )
                                
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Hub,
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "COMMUNITY CONTEXT",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = ctx.clusterMessage,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isCluster) FontWeight.Bold else FontWeight.Normal,
                                        color = White
                                    )
                                }
                            }
                        }
                    }

                    // 4. Recommended Action Section
                    Text(
                        text = "Recommended Action",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalBlack
                    )

                    when (diagnosis.riskLevel) {
                        RiskLevel.EMERGENCY -> {
                            ActionCard("Go to nearest PHC immediately", Icons.Rounded.LocalHospital, CoralRed)
                            ActionCard("Call 108 Ambulance", Icons.Rounded.Phone, CoralRed)
                            ActionCard("Share summary with doctor", Icons.Rounded.Share, WarmGrey)
                        }
                        RiskLevel.URGENT -> {
                            ActionCard("Visit health center within 24 hours", Icons.Rounded.LocalHospital, WarmAmber)
                            ActionCard("Monitor temperature every 4 hours", Icons.Rounded.MonitorHeart, SageGreen)
                            ActionCard("Set follow-up reminder", Icons.Rounded.Alarm, WarmGrey)
                        }
                        RiskLevel.NORMAL -> {
                            ActionCard("Rest and home management", Icons.Rounded.Home, SageGreen)
                            ActionCard("Check back in 3 days", Icons.Rounded.Event, WarmGrey)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save Patient Record
                    Button(
                        onClick = { onNavigateToSummary(consultationId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurmericGold),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Save Patient Record",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalBlack
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ConditionCard(condition: ConditionResult, riskLevel: RiskLevel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = condition.nameLocal,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = CharcoalBlack
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = condition.nameEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = WarmGrey
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visual Confidence Bar
            val barColor = when {
                condition.confidence >= 0.8f -> TurmericGold
                condition.confidence >= 0.6f -> SageGreen
                else -> LightGrey
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ParchmentDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(condition.confidence)
                        .fillMaxHeight()
                        .background(barColor)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = condition.description,
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalBlack
            )
        }
    }
}

@Composable
fun ActionCard(text: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = CharcoalBlack
            )
        }
    }
}
