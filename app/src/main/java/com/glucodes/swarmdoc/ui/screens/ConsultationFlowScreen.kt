package com.glucodes.swarmdoc.ui.screens

import android.content.Intent
import android.view.SoundEffectConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream
import com.glucodes.swarmdoc.ml.audio.SpeechRecognizerManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.glucodes.swarmdoc.util.ImageUtils
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.Constants
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.ConsultationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationFlowScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSos: () -> Unit = {},
    viewModel: ConsultationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        // Top bar with back and step indicator
        TopAppBar(
            title = { Text(LocalStrings.current.consultation, fontWeight = FontWeight.Bold, color = ForestGreen) },
            navigationIcon = {
                IconButton(onClick = {
                    if (state.currentStep > 1) viewModel.previousStep()
                    else onNavigateBack()
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        // Step progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (i in 1..state.totalSteps) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                i < state.currentStep -> ForestGreen
                                i == state.currentStep -> TurmericGold
                                else -> WarmGrey.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        Text(
        "${LocalStrings.current.next} ${state.currentStep} / ${state.totalSteps}",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = WarmGrey,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            when (state.currentStep) {
                1 -> Step1PatientBasics(state, viewModel)
                2 -> Step2InputMode(state, viewModel)
                3 -> Step3Symptoms(state, viewModel)
                4 -> Step4Diagnosis(state, viewModel)
                5 -> Step5Action(state, viewModel, onNavigateBack, onNavigateToSos, context)
            }
        }
    }
}

@Composable
private fun Step1PatientBasics(
    state: com.glucodes.swarmdoc.viewmodel.ConsultationUiState,
    viewModel: ConsultationViewModel,
) {
    Text(LocalStrings.current.patientDetails, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Spacer(modifier = Modifier.height(20.dp))

    // Name
    OutlinedTextField(
        value = state.patientName,
        onValueChange = { viewModel.updatePatientName(it) },
        label = { Text(LocalStrings.current.patientNameOptional) },
        modifier = Modifier.fillMaxWidth(),
        colors = swarmDocTextFieldColors(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(16.dp))

    // Age - large number display with +/- buttons
    Text(LocalStrings.current.age, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        FilledIconButton(
            onClick = { viewModel.updatePatientAge((state.patientAge - 1).coerceAtLeast(0)) },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ParchmentDark),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = CharcoalBlack)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            "${state.patientAge}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = ForestGreen,
        )
        Spacer(modifier = Modifier.width(24.dp))
        FilledIconButton(
            onClick = { viewModel.updatePatientAge((state.patientAge + 1).coerceAtMost(100)) },
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = ParchmentDark),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = CharcoalBlack)
        }
    }
    Text(
        "${LocalStrings.current.years}",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelSmall,
        color = WarmGrey,
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Sex - three large segmented buttons
    Text(LocalStrings.current.sex, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        listOf("MALE" to LocalStrings.current.male, "FEMALE" to LocalStrings.current.female, "OTHER" to LocalStrings.current.other).forEach { (value, label) ->
            val selected = state.patientSex == value
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable { viewModel.updatePatientSex(value) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) ForestGreen else White
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (selected) null else BorderStroke(1.dp, WarmGrey.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        when (value) { "MALE" -> Icons.Rounded.Male; "FEMALE" -> Icons.Rounded.Female; else -> Icons.Rounded.Transgender },
                        contentDescription = label,
                        tint = if (selected) White else CharcoalBlack,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(label, color = if (selected) White else CharcoalBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }

    // Privacy mode toggle (only for Female)
    if (state.patientSex == "FEMALE") {
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = PrivacyIndigo, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(LocalStrings.current.femalePrivacyMode, style = MaterialTheme.typography.bodyMedium, color = PrivacyIndigo)
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = state.privacyMode, onCheckedChange = { viewModel.togglePrivacyMode(it) })
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Village & District
    OutlinedTextField(
        value = state.village,
        onValueChange = { viewModel.updateVillage(it) },
        label = { Text(LocalStrings.current.village) },
        modifier = Modifier.fillMaxWidth(),
        colors = swarmDocTextFieldColors(),
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = state.district,
        onValueChange = { viewModel.updateDistrict(it) },
        label = { Text(LocalStrings.current.district) },
        modifier = Modifier.fillMaxWidth(),
        colors = swarmDocTextFieldColors(),
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Validation: require age > 0, sex selected, and village filled
    val step1Valid = state.patientAge > 0 && state.patientSex.isNotBlank() && state.village.isNotBlank()
    if (!step1Valid) {
        Text(
            LocalStrings.current.fillRequiredFields,
            style = MaterialTheme.typography.bodySmall,
            color = CoralRed,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    val view = LocalView.current
    Button(
        onClick = {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            viewModel.nextStep()
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = step1Valid,
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(LocalStrings.current.nextChooseInputMode, fontWeight = FontWeight.Bold, color = White)
    }
}

@Composable
private fun Step2InputMode(
    state: com.glucodes.swarmdoc.viewmodel.ConsultationUiState,
    viewModel: ConsultationViewModel,
) {
    var showVoiceOverlay by remember { mutableStateOf(false) }
    var showPhotoOverlay by remember { mutableStateOf(false) }

    Text(LocalStrings.current.howToDescribeSymptoms, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Text(LocalStrings.current.selectFromSymptomList, style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
    Spacer(modifier = Modifier.height(20.dp))

    // Voice card
    InputModeCard(
        title = LocalStrings.current.describeByVoice,
        subtitle = LocalStrings.current.speakSymptomsInYourLang,
        icon = Icons.Rounded.Mic,
        color = TurmericGold,
        isCompleted = state.voiceCompleted,
        onClick = { showVoiceOverlay = true },
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Text card
    InputModeCard(
        title = LocalStrings.current.typeSymptoms,
        subtitle = LocalStrings.current.selectFromSymptomList,
        icon = Icons.Rounded.Keyboard,
        color = ForestGreen,
        isCompleted = state.textCompleted,
        onClick = { viewModel.markTextCompleted(); viewModel.nextStep() },
    )
    Spacer(modifier = Modifier.height(12.dp))

    // Photo card
    InputModeCard(
        title = LocalStrings.current.capturePhoto,
        subtitle = LocalStrings.current.woundSkinEyeReport,
        icon = Icons.Rounded.CameraAlt,
        color = CoralRed,
        isCompleted = state.photoCompleted,
        onClick = { showPhotoOverlay = true },
    )

    Spacer(modifier = Modifier.height(24.dp))

    val anyModeUsed = state.voiceCompleted || state.textCompleted || state.photoCompleted
    if (anyModeUsed) {
        Button(
            onClick = { viewModel.nextStep() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(LocalStrings.current.continueToSymptoms, fontWeight = FontWeight.Bold, color = White)
        }
    }

    if (showVoiceOverlay) {
        VoiceInputOverlay(
            onDismiss = { showVoiceOverlay = false },
            onComplete = { text ->
                viewModel.updateNotes(state.additionalNotes + "\n[Voice Note]: $text")
                viewModel.markVoiceCompleted()
                showVoiceOverlay = false
            }
        )
    }

    if (showPhotoOverlay) {
        PhotoInputOverlay(
            onDismiss = { showPhotoOverlay = false },
            onComplete = { path ->
                viewModel.markPhotoCompleted(path)
                showPhotoOverlay = false
            }
        )
    }
}

@Composable
private fun InputModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isCompleted: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) color.copy(alpha = 0.1f) else White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isCompleted) color else WarmGrey.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
            }
            if (isCompleted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Done", tint = ForestGreen, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun Step3Symptoms(
    state: com.glucodes.swarmdoc.viewmodel.ConsultationUiState,
    viewModel: ConsultationViewModel,
) {
    val symptoms = viewModel.getSymptomListForPatient()
    val privacyMode = state.privacyMode

    Text(LocalStrings.current.selectSymptoms, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Text(LocalStrings.current.tapAllSymptoms, style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
    Spacer(modifier = Modifier.height(16.dp))

    // Symptom chips
    FlowRow(symptoms, state.selectedSymptoms, privacyMode, viewModel)

    Spacer(modifier = Modifier.height(16.dp))

    // Duration
    Text(LocalStrings.current.durationDays, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        FilledIconButton(onClick = { viewModel.updateDuration((state.durationDays - 1).coerceAtLeast(1)) }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ParchmentDark)) {
            Icon(Icons.Rounded.Remove, contentDescription = null, tint = CharcoalBlack)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text("${state.durationDays}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ForestGreen)
        Spacer(modifier = Modifier.width(16.dp))
        FilledIconButton(onClick = { viewModel.updateDuration(state.durationDays + 1) }, modifier = Modifier.size(40.dp), colors = IconButtonDefaults.filledIconButtonColors(containerColor = ParchmentDark)) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = CharcoalBlack)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Vitals
    Text(LocalStrings.current.vitalsOptional, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = state.temperature, onValueChange = { viewModel.updateTemperature(it) }, label = { Text("Temp (C)") }, modifier = Modifier.weight(1f), colors = swarmDocTextFieldColors(), singleLine = true)
        OutlinedTextField(value = state.pulseRate, onValueChange = { viewModel.updatePulseRate(it) }, label = { Text("Pulse") }, modifier = Modifier.weight(1f), colors = swarmDocTextFieldColors(), singleLine = true)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = state.systolicBP, onValueChange = { viewModel.updateSystolicBP(it) }, label = { Text("BP Sys") }, modifier = Modifier.weight(1f), colors = swarmDocTextFieldColors(), singleLine = true)
        OutlinedTextField(value = state.diastolicBP, onValueChange = { viewModel.updateDiastolicBP(it) }, label = { Text("BP Dia") }, modifier = Modifier.weight(1f), colors = swarmDocTextFieldColors(), singleLine = true)
        OutlinedTextField(value = state.spo2, onValueChange = { viewModel.updateSpo2(it) }, label = { Text("SpO2") }, modifier = Modifier.weight(1f), colors = swarmDocTextFieldColors(), singleLine = true)
    }

    // Women's Health section (collapsible, female only)
    if (state.patientSex == "FEMALE" && Constants.getAgeGroup(state.patientAge) == Constants.AGE_GROUP_ADULT) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleWomensHealth() },
            colors = CardDefaults.cardColors(containerColor = if (privacyMode) PrivacyIndigoLight.copy(alpha = 0.1f) else White),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = PrivacyIndigo, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (privacyMode) "Additional Screening" else "Women's Health", fontWeight = FontWeight.Bold, color = CharcoalBlack)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        if (state.expandedWomensHealth) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null, tint = WarmGrey
                    )
                }
                if (state.expandedWomensHealth) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Constants.FEMALE_HEALTH_SYMPTOMS.forEach { symptom ->
                        val label = if (privacyMode) Constants.PRIVACY_CODED_SYMPTOMS[symptom] ?: symptom else Constants.SYMPTOM_DISPLAY_NAMES[symptom] ?: symptom
                        val selected = symptom in state.selectedSymptoms
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleSymptom(symptom) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = selected, onCheckedChange = { viewModel.toggleSymptom(symptom) })
                            Text(label, color = CharcoalBlack)
                        }
                    }
                }
            }
        }
    }

    // Advanced Checks section (collapsible)
    Spacer(modifier = Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleAdvancedChecks() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Science, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Run additional checks (optional)", fontWeight = FontWeight.Bold, color = CharcoalBlack)
                Spacer(modifier = Modifier.weight(1f))
                Icon(if (state.expandedAdvancedChecks) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null, tint = WarmGrey)
            }
            if (state.expandedAdvancedChecks) {
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "Cough Screener" to "Acoustic cough analysis",
                    "Anemia Eye Check" to "Pallor detection from eye photo",
                    "rPPG Heart Rate" to "Camera-based pulse measurement",
                    "Capillary Refill SpO2" to "Fingertip oxygen estimation",
                ).forEach { (title, desc) ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { /* launch specific check */ },
                        colors = CardDefaults.cardColors(containerColor = Parchment),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CharcoalBlack)
                                Text(desc, fontSize = 12.sp, color = WarmGrey)
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = ForestGreen)
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Notes
    OutlinedTextField(
        value = state.additionalNotes,
        onValueChange = { viewModel.updateNotes(it) },
        label = { Text(LocalStrings.current.additionalNotes) },
        modifier = Modifier.fillMaxWidth().height(80.dp),
        colors = swarmDocTextFieldColors(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { viewModel.runDiagnosis() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        enabled = state.selectedSymptoms.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(LocalStrings.current.runDiagnosis, fontWeight = FontWeight.Bold, color = White)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun FlowRow(
    symptoms: List<String>,
    selected: Set<String>,
    privacyMode: Boolean,
    viewModel: ConsultationViewModel,
) {
    // Simple wrapping layout using multiple rows
    val chunked = symptoms.chunked(3)
    chunked.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { symptom ->
                val isSelected = symptom in selected
                val displayName = Constants.SYMPTOM_DISPLAY_NAMES[symptom] ?: symptom.replace("_", " ").replaceFirstChar { it.uppercase() }
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.toggleSymptom(symptom) },
                    label = { Text(displayName, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreen,
                        selectedLabelColor = White,
                    ),
                )
            }
            // Fill remaining space if row has fewer than 3 items
            repeat(3 - row.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Step4Diagnosis(
    state: com.glucodes.swarmdoc.viewmodel.ConsultationUiState,
    viewModel: ConsultationViewModel,
) {
    if (state.isAnalyzing) {
        // Analysis animation
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val anim = rememberInfiniteTransition(label = "analyzing")
            val scale by anim.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "scale"
            )
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size((80 * scale).dp)
                        .clip(CircleShape)
                        .background(ForestGreen.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size((60 * scale).dp)
                        .clip(CircleShape)
                        .background(ForestGreen.copy(alpha = 0.25f))
                )
                Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(LocalStrings.current.analyzing, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ForestGreen)
            Text(LocalStrings.current.runningDiagnosticAssessment, style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
        }
        return
    }

    val result = state.diagnosisResult ?: return

    // Risk badge
    val riskColor = when (result.riskLevel) {
        com.glucodes.swarmdoc.ml.triage.RiskLevel.EMERGENCY -> CoralRed
        com.glucodes.swarmdoc.ml.triage.RiskLevel.URGENT -> AmberOrange
        com.glucodes.swarmdoc.ml.triage.RiskLevel.NORMAL -> SageGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(riskColor))
                Spacer(modifier = Modifier.width(12.dp))
                Text(result.riskLevel.name, fontWeight = FontWeight.Bold, color = riskColor, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(result.recommendedAction, color = CharcoalBlack, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Top conditions
    Text(LocalStrings.current.possibleConditions, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
    Spacer(modifier = Modifier.height(8.dp))
    result.topConditions.forEach { condition ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(condition.conditionEnglish, fontWeight = FontWeight.Bold, color = CharcoalBlack, modifier = Modifier.weight(1f))
                    Text("${(condition.confidence * 100).toInt()}%", fontWeight = FontWeight.Bold, color = ForestGreen)
                }
                Text(condition.conditionLocal, style = MaterialTheme.typography.labelSmall, color = WarmGrey)
                Spacer(modifier = Modifier.height(4.dp))
                Text(condition.description, style = MaterialTheme.typography.bodySmall, color = CharcoalBlack)
                // Confidence bar
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { condition.confidence },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = ForestGreen,
                    trackColor = ParchmentDark,
                )
            }
        }
    }

    // Community context
    if (state.communityClusterDetected) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = CoralRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(LocalStrings.current.communityAlert, fontWeight = FontWeight.Bold, color = CoralRed)
                    Text(state.clusterMessage, style = MaterialTheme.typography.bodySmall, color = CharcoalBlack)
                }
            }
        }
    }

    // Medicine recommendations
    if (state.recommendedMedicines.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(LocalStrings.current.recommendedMedicines, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
        Spacer(modifier = Modifier.height(8.dp))
        state.recommendedMedicines.forEach { med ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(med.genericName, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                        Text(med.dose, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                    }
                    if (med.inStock) {
                        Text(LocalStrings.current.inStock, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SageGreen,
                            modifier = Modifier.background(SageGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    } else {
                        Text(LocalStrings.current.outOfStock, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CoralRed,
                            modifier = Modifier.background(CoralRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { viewModel.saveConsultation() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(LocalStrings.current.saveAndContinue, fontWeight = FontWeight.Bold, color = White)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun Step5Action(
    state: com.glucodes.swarmdoc.viewmodel.ConsultationUiState,
    viewModel: ConsultationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSos: () -> Unit,
    context: android.content.Context,
) {
    val riskLevel = state.diagnosisResult?.riskLevel

    Text(LocalStrings.current.consultationSaved, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ForestGreen)
    Spacer(modifier = Modifier.height(4.dp))
    Text("Record #${state.savedConsultationId}", style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
    Spacer(modifier = Modifier.height(24.dp))

    // SOS button for Emergency
    if (riskLevel == com.glucodes.swarmdoc.ml.triage.RiskLevel.EMERGENCY) {
        Button(
            onClick = { onNavigateToSos() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(LocalStrings.current.sosAlertNearestDoctor, fontWeight = FontWeight.Bold, color = White)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Refer to PHC
    if (riskLevel == com.glucodes.swarmdoc.ml.triage.RiskLevel.EMERGENCY || riskLevel == com.glucodes.swarmdoc.ml.triage.RiskLevel.URGENT) {
        OutlinedButton(
            onClick = { /* referral logic */ },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AmberOrange),
        ) {
            Text(LocalStrings.current.referToPHC, fontWeight = FontWeight.Bold, color = AmberOrange)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Share with Doctor
    OutlinedButton(
        onClick = {
            val shareText = viewModel.generateShareText()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                setPackage("com.whatsapp")
            }
            try { context.startActivity(intent) } catch (_: Exception) {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(fallback, "Share with Doctor"))
            }
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Rounded.Share, contentDescription = null, tint = ForestGreen)
        Spacer(modifier = Modifier.width(8.dp))
        Text(LocalStrings.current.shareWithDoctor, fontWeight = FontWeight.Bold, color = ForestGreen)
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = { onNavigateBack() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(LocalStrings.current.returnToHome, fontWeight = FontWeight.Bold, color = White)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

// Change 9: Consistent TextField colors throughout the app
@Composable
fun swarmDocTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(Constants.TEXT_COLOR.toInt()),
    unfocusedTextColor = Color(Constants.TEXT_COLOR.toInt()),
    focusedContainerColor = Color(Constants.TEXT_FIELD_CONTAINER.toInt()),
    unfocusedContainerColor = Color(Constants.TEXT_FIELD_CONTAINER.toInt()),
    cursorColor = Color(Constants.CURSOR_COLOR.toInt()),
    focusedLabelColor = Color(Constants.LABEL_COLOR.toInt()),
    unfocusedLabelColor = Color(Constants.LABEL_COLOR.toInt()),
    focusedBorderColor = Color(Constants.FOCUSED_BORDER_COLOR.toInt()),
    unfocusedBorderColor = Color(Constants.UNFOCUSED_BORDER_COLOR.toInt()),
)

@Composable
fun VoiceInputOverlay(
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val speechManager = remember { SpeechRecognizerManager(context) }
    
    val hasMic = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    var isRecording by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    val isSpeechAvailable by speechManager.isAvailable.collectAsState()

    DisposableEffect(Unit) {
        onDispose { speechManager.release() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(CharcoalBlack.copy(alpha = 0.95f)).padding(24.dp)) {
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = White)
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Dictate Clinical Notes", style = MaterialTheme.typography.headlineSmall, color = TurmericGold)
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(if (isRecording) CoralRed else TurmericGold).clickable {
                        if (!hasMic) return@clickable
                        if (isRecording) {
                            speechManager.stopListening()
                            isRecording = false
                        } else {
                            transcriptionText = ""
                            speechManager.startListening(continuous = true)
                                .onEach { (text, _) -> transcriptionText = text }
                                .launchIn(coroutineScope)
                            isRecording = true
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = "Record", tint = if (isRecording) White else CharcoalBlack, modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = transcriptionText.ifEmpty { if (hasMic) "Tap to start speaking" else "Microphone permission required" },
                    color = White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (transcriptionText.isNotBlank()) {
                    Button(
                        onClick = { onComplete(transcriptionText) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                    ) {
                        Text("Save Note", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoInputOverlay(
    onDismiss: () -> Unit,
    onComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    val cameraProviderFuture = remember { if (hasCamera) ProcessCameraProvider.getInstance(context) else null }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize().background(CharcoalBlack)) {
            if (hasCamera && cameraProviderFuture != null) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            imageCapture = ImageCapture.Builder().build()
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                        } catch (e: Exception) { android.util.Log.e("Camera", "Binding failed", e) }
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Camera permission required", color = White, modifier = Modifier.align(Alignment.Center))
            }
            
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopStart).padding(24.dp).background(CharcoalBlack.copy(0.5f), CircleShape)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = White)
            }
            
            IconButton(
                onClick = {
                    val currentCapture = imageCapture ?: return@IconButton
                    currentCapture.takePicture(
                        executor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = convertImageProxyToBitmap(image)
                                image.close()
                                bitmap?.let { b ->
                                    coroutineScope.launch {
                                        val path = ImageUtils.saveBitmapToDevice(context, b, "ConsultationPhoto_${System.currentTimeMillis()}.jpg")
                                        onComplete(path ?: "")
                                    }
                                }
                            }
                        }
                    )
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp).size(80.dp).background(White, CircleShape).border(4.dp, ForestGreen, CircleShape)
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture", tint = CharcoalBlack, modifier = Modifier.size(32.dp))
            }
        }
    }
}

// Minimal clone of the helper to avoid circular deps with SpecialDiagnostics
fun convertImageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val format = image.format
        if (format == ImageFormat.JPEG) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else if (format == ImageFormat.YUV_420_888) {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val jpegBytes = out.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return null
            val rotation = image.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else bitmap
        } else {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (e: Exception) { null }
}

