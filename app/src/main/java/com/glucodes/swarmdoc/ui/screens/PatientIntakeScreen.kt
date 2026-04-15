package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.domain.models.InputMode
import com.glucodes.swarmdoc.ui.components.SymptomChip
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.Constants
import com.glucodes.swarmdoc.viewmodel.IntakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientIntakeScreen(
    privacyMode: Boolean,
    onPrivacyModeChange: (Boolean) -> Unit,
    onNavigateToDiagnosis: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    val isSaving by viewModel.isSaving.collectAsState()
    val savedId by viewModel.savedConsultationId.collectAsState()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("FEMALE") }
    var village by remember { mutableStateOf("") }

    val selectedSymptoms = remember { mutableStateListOf<String>() }
    var additionalNotes by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf(1f) }

    val scrollState = rememberScrollState()

    LaunchedEffect(savedId) {
        if (savedId != null) {
            onNavigateToDiagnosis(savedId!!)
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Intake") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (privacyMode) PrivacySurface else Parchment,
                    titleContentColor = if (privacyMode) PrivacyIndigo else CharcoalBlack,
                ),
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
            // Step 1: Basics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Patient Basics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalBlack
                        )

                        // Privacy Mode Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (privacyMode) PrivacyIndigo else WarmGrey,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = privacyMode,
                                onCheckedChange = onPrivacyModeChange,
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrivacyLavender,
                                    checkedTrackColor = PrivacyIndigoLight,
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!privacyMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = village,
                            onValueChange = { village = it },
                            label = { Text("Village / Location") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sex segmented buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ParchmentDark),
                    ) {
                        listOf("MALE", "FEMALE", "OTHER").forEach { option ->
                            val isSelected = sex == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isSelected) TurmericGold else Color.Transparent)
                                    .clickable { sex = option },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) CharcoalBlack else WarmGrey,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Symptoms
            Text(
                text = "Symptoms",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CharcoalBlack
            )

            // Dynamic grid configuration based on symptoms
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Constants.SYMPTOM_LIST.forEach { sym ->
                    val isSecretive = privacyMode && sym in listOf("vaginal_discharge", "menstrual_irregularity", "breast_symptoms")
                    val displayName = if (isSecretive) {
                        Constants.PRIVACY_CODED_SYMPTOMS[sym] ?: sym
                    } else {
                        Constants.SYMPTOM_DISPLAY_NAMES[sym] ?: sym
                    }
                    val isSelected = selectedSymptoms.contains(sym)

                    SymptomChip(
                        name = sym,
                        displayName = displayName,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) selectedSymptoms.remove(sym)
                            else selectedSymptoms.add(sym)
                        }
                    )
                }
            }

            OutlinedTextField(
                value = additionalNotes,
                onValueChange = { additionalNotes = it },
                label = { Text("Additional Symptom Details") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Duration: ${durationDays.toInt()} days", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = durationDays,
                onValueChange = { durationDays = it },
                valueRange = 1f..30f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = TurmericGold,
                    activeTrackColor = ForestGreen
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.saveIntakeAndProceed(
                        name = if (privacyMode) "" else name,
                        age = age.toIntOrNull() ?: 0,
                        sex = sex,
                        village = village,
                        district = "Puri", // Hardcoded for demo
                        selectedSymptoms = selectedSymptoms,
                        additionalSymptoms = additionalNotes,
                        durationDays = durationDays.toInt(),
                        temperature = null, // simplified for demo
                        recentTravel = false,
                        inputMode = InputMode.TEXT
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving && selectedSymptoms.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor =  if (privacyMode) PrivacyIndigo else ForestGreen,
                    contentColor = White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        "Analyze Symptoms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Simple FlowRow equivalent for quick layout
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // In Compose 1.5+ FlowRow is available in foundation.layout.
    // Assuming we have it available in the classpath.
    @OptIn(ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
