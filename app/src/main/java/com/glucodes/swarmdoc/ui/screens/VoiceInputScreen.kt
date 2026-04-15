package com.glucodes.swarmdoc.ui.screens
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.domain.models.InputMode
import com.glucodes.swarmdoc.ml.audio.SpeechRecognizerManager
import com.glucodes.swarmdoc.ml.llm.RegexExtractor
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.IntakeViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputScreen(
    onNavigateToDiagnosis: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val speechManager = remember { SpeechRecognizerManager(context) }
    val regexExtractor = remember { RegexExtractor() }
    val hasMic = rememberHasMicrophone()
    
    var isRecording by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf("") }
    var extractedData by remember { mutableStateOf<RegexExtractor.ExtractedData?>(null) }
    
    val isSaving by viewModel.isSaving.collectAsState()
    val savedId by viewModel.savedConsultationId.collectAsState()

    LaunchedEffect(savedId) {
        if (savedId != null) {
            onNavigateToDiagnosis(savedId!!)
            viewModel.resetState()
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechManager.release() }
    }

    val isSpeechAvailable by speechManager.isAvailable.collectAsState()
    val canRecord = hasMic && isSpeechAvailable

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Speak Symptoms") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ForestGreen, titleContentColor = White, navigationIconContentColor = White),
            )
        },
        containerColor = ForestGreen
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (!canRecord && !showReview) {
                DemoModeBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!showReview) {
                // Waveform Visualizer
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    if (isRecording) {
                        VoiceWaveformVisualizer()
                    } else {
                        Text(
                            text = if (!canRecord) "Microphone not available on this device" else "Tap microphone to dictate clinical notes",
                            style = MaterialTheme.typography.titleLarge, color = TurmericGoldLight, textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                
                Text(
                    text = transcriptionText.ifEmpty { "..." },
                    style = MaterialTheme.typography.headlineSmall, color = White, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp).heightIn(min = 100.dp)
                )

                Spacer(modifier = Modifier.weight(1f, fill=false))
                Spacer(modifier = Modifier.height(40.dp))

                if (canRecord) {
                    // Real mic button
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(if (isRecording) CoralRed else TurmericGold).clickable {
                            if (isRecording) {
                                speechManager.stopListening()
                                isRecording = false
                                extractedData = regexExtractor.extract(transcriptionText)
                                showReview = true
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
                            contentDescription = "Record", tint = if (isRecording) White else CharcoalBlack, modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    // Demo dictation button
                    Button(
                        onClick = {
                            transcriptionText = "Patient has fever for 5 days with headache and joint pain. She also has vomiting and loss of appetite. Temperature was measured at 102 degrees."
                            extractedData = regexExtractor.extract(transcriptionText)
                            showReview = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TurmericGold),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Mic, contentDescription = null, tint = CharcoalBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Demo Dictation", color = CharcoalBlack, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Review Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Structured Extract (Regex+STT)", style = MaterialTheme.typography.labelMedium, color = WarmGrey)
                            IconButton(onClick = {
                                val text = "*Initial Clinical Note*\nSymptoms: ${extractedData?.symptoms?.joinToString()}\nDuration: ${extractedData?.duration} days\nVitals: ${extractedData?.vitals}\nRaw: $transcriptionText"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    setPackage("com.whatsapp")
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {
                                    // WhatsApp not installed, fallback to generic share
                                    val fallback = Intent.createChooser(Intent(Intent.ACTION_SEND).apply { 
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) 
                                    }, "Share Clinical Note")
                                    context.startActivity(fallback)
                                }
                            }) { Icon(Icons.Rounded.Share, contentDescription = "Share", tint = ForestGreen) }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Highlight entities in the transcription
                        val annotatedStr = buildAnnotatedString {
                            val lowerText = transcriptionText.lowercase()
                            var lastIndex = 0
                            
                            // A very simplistic highlighter for demo purposes. 
                            // Real app would use the spans from the regex extractor directly.
                            val highlights = mutableListOf<Pair<IntRange, Color>>()
                            extractedData?.symptoms?.forEach { sym ->
                                val idx = lowerText.indexOf(sym)
                                if (idx >= 0) highlights.add((idx until idx+sym.length) to Color.Blue)
                            }
                            
                            highlights.sortBy { it.first.first }
                            
                            var currentIdx = 0
                            for (hw in highlights) {
                                if (hw.first.first >= currentIdx) {
                                    append(transcriptionText.substring(currentIdx, hw.first.first))
                                    withStyle(style = SpanStyle(background = hw.second.copy(alpha=0.3f), fontWeight = FontWeight.Bold, color = hw.second)) {
                                        append(transcriptionText.substring(hw.first.first, hw.first.last + 1))
                                    }
                                    currentIdx = hw.first.last + 1
                                }
                            }
                            if (currentIdx < transcriptionText.length) {
                                append(transcriptionText.substring(currentIdx))
                            }
                            if (highlights.isEmpty()) append(transcriptionText)
                        }
                        
                        Text(text = annotatedStr, style = MaterialTheme.typography.bodyLarge, color = CharcoalBlack)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = transcriptionText,
                            onValueChange = { transcriptionText = it; extractedData = regexExtractor.extract(it) },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Edit Raw Text") },
                            colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = ForestGreen)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val durationText = extractedData?.duration ?: ""
                        val durationNum = durationText.filter { it.isDigit() }.toIntOrNull() ?: 1
                        viewModel.saveIntakeAndProceed(
                            name = "", age = 30, sex = "MALE", village = "Unknown", district = "Puri",
                            selectedSymptoms = extractedData?.symptoms?.toList() ?: emptyList(),
                            additionalSymptoms = transcriptionText,
                            durationDays = durationNum, temperature = null, recentTravel = false,
                            inputMode = InputMode.VOICE
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold), enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = CharcoalBlack, modifier = Modifier.size(24.dp))
                    else Text("Save & Diagnose", style = MaterialTheme.typography.titleMedium, color = CharcoalBlack, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { showReview = false; isRecording = false; transcriptionText = "" }) {
                    Text("Record Again", color = White)
                }
            }
        }
    }
}

@Composable
fun VoiceWaveformVisualizer() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing)
        ), label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width; val height = size.height; val centerY = height / 2f
        val barCount = 40; val barWidth = width / (barCount * 2)
        
        for (i in 0 until barCount) {
            val x = i * barWidth * 2 + barWidth / 2
            val noise = kotlin.math.sin(phase + i * 0.5f) * kotlin.math.sin(phase * 1.5f + i * 0.2f)
            val amplitude = (height * 0.4f * kotlin.math.abs(noise).toFloat() + height * 0.05f)
            drawLine(color = TurmericGold, start = Offset(x, centerY - amplitude), end = Offset(x, centerY + amplitude), strokeWidth = barWidth, cap = StrokeCap.Round)
        }
    }
}
