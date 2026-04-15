package com.glucodes.swarmdoc.ui.screens

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.glucodes.swarmdoc.ml.vision.CapillaryRefillProcessor
import com.glucodes.swarmdoc.ml.vision.ConjunctivalPallorAnalyzer
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.AnemiaScreenerViewModel
import com.glucodes.swarmdoc.viewmodel.AnemiaUiState
import com.glucodes.swarmdoc.viewmodel.CoughScreenerViewModel
import com.glucodes.swarmdoc.viewmodel.CoughUiState
import com.glucodes.swarmdoc.viewmodel.RppgUiState
import com.glucodes.swarmdoc.viewmodel.RppgViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

// -------------------------------------------------------------------------------------------------
// DEMO MODE BADGE
// -------------------------------------------------------------------------------------------------
@Composable
fun DemoModeBadge(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = WarmAmber.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmAmber)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Smartphone, contentDescription = null, tint = WarmAmber, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DEMO MODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = WarmAmber)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// HARDWARE CHECK HELPERS
// -------------------------------------------------------------------------------------------------
@Composable
fun rememberHasMicrophone(): Boolean {
    val context = LocalContext.current
    return remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun rememberHasCamera(): Boolean {
    val context = LocalContext.current
    return remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

// -------------------------------------------------------------------------------------------------
// COUGH SCREENER
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoughScreenerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CoughScreenerViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val hasMic = rememberHasMicrophone()
    val elapsedSec = (elapsedMs / 1000f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.coughScreener) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack, titleContentColor = White, navigationIconContentColor = White)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasMic) {
                DemoModeBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(strings.coughIntoMicrophone, color = White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))

            when (val state = uiState) {
                is CoughUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        if (hasMic) {
                            Button(onClick = { viewModel.startRecording() }, colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = CharcoalBlack)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.startRecording, color = CharcoalBlack)
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Microphone not available on this device.", color = WarmGrey, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.runDemoAnalysis() }, colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Run Demo Analysis", color = CharcoalBlack)
                                }
                            }
                        }
                    }
                }
                is CoughUiState.Recording -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        VolumeVisualizer(amplitude = if (hasMic) amplitude else 0.5f)
                    }
                    Text("${strings.recording} ${"%.1f".format(elapsedSec)}s", color = TurmericGold, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.submitRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = CharcoalBlack)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.submitAudio, color = CharcoalBlack)
                        }
                        OutlinedButton(onClick = { viewModel.rerecord() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.rerecord)
                        }
                    }
                }
                is CoughUiState.Analyzing -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TurmericGold)
                    }
                    Text("Analyzing Spectral Features...", color = White)
                }
                is CoughUiState.Result -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (state.tbSuspected) CoralRed.copy(alpha = 0.2f) else ForestGreen.copy(alpha=0.2f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (state.tbSuspected) CoralRed else ForestGreen)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!hasMic) { DemoModeBadge(); Spacer(modifier = Modifier.height(8.dp)) }
                            Text(
                                "Result: ${if (state.isCoughDetected) state.coughType else "No Cough Detected"}",
                                color = White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Confidence: ${(state.confidence * 100).toInt()}%", color = WarmGrey)
                            if (state.recommendation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(state.recommendation, color = White.copy(alpha = 0.9f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (state.tbSuspected) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("⚠️ TB SUSPECTED", color = CoralRed, fontWeight = FontWeight.Bold)
                                Text("Persistent cough pattern detected. Refer for sputum test.", color = White, textAlign = TextAlign.Center, modifier = Modifier.padding(top=4.dp))
                            }
                        }
                    }
                }
                is CoughUiState.Error -> {
                    Text(state.message, color = CoralRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!hasMic) {
                        Button(onClick = { viewModel.runDemoAnalysis() }, colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)) {
                            Text("Try Demo Analysis", color = CharcoalBlack)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is CoughUiState.Result || uiState is CoughUiState.Error) {
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGrey)
                ) {
                    Text(strings.restart)
                }
            }
        }
    }
}

@Composable
fun VolumeVisualizer(amplitude: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val height = size.height
        val width = size.width
        val barWidth = width * 0.1f
        val maxBars = 5
        val gap = (width - (barWidth * maxBars)) / (maxBars + 1)
        
        for (i in 0 until maxBars) {
            val startX = gap + i * (barWidth + gap)
            val barAmp = (amplitude * height) * (1f - Math.abs(i - 2) * 0.2f)
            val h = barAmp.coerceIn(10f, height * 0.8f)
            
            drawLine(
                color = TurmericGold,
                start = Offset(startX + barWidth/2, height/2 - h/2),
                end = Offset(startX + barWidth/2, height/2 + h/2),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// -------------------------------------------------------------------------------------------------
// ANEMIA EYE CHECK
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnemiaEyeCheckScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnemiaScreenerViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera = rememberHasCamera()
    val cameraProviderFuture = remember { if (hasCamera) ProcessCameraProvider.getInstance(context) else null }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isFrontCamera by remember { mutableStateOf(true) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.anemiaEyeCheck) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack, titleContentColor = White, navigationIconContentColor = White)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasCamera) {
                DemoModeBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Align lower eyelid in oval frame and pull down slightly", color = White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is AnemiaUiState.Idle -> {
                    if (hasCamera && cameraProviderFuture != null) {
                        Box(modifier = Modifier.size(300.dp)) {
                            key(isFrontCamera) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                            imageCapture = ImageCapture.Builder().build()
                                            val cameraSelector = if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                                        } catch (e: Exception) { Log.e("Camera", "Binding failed", e) }
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                                )
                            }
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawOval(color = TurmericGold, topLeft = Offset(size.width*0.2f, size.height*0.4f), size = androidx.compose.ui.geometry.Size(size.width*0.6f, size.height*0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                            }
                            IconButton(
                                onClick = {
                                    val currentImageCapture = imageCapture ?: return@IconButton
                                    currentImageCapture.takePicture(
                                        executor,
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val bitmap = imageProxyToBitmap(image)
                                                image.close()
                                                bitmap?.let { b ->
                                                    coroutineScope.launch {
                                                        com.glucodes.swarmdoc.util.ImageUtils.saveBitmapToDevice(context, b, "AnemiaCheck_${System.currentTimeMillis()}.jpg")
                                                    }
                                                    viewModel.analyzeImage(b)
                                                }
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(64.dp).background(TurmericGold, CircleShape)
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture", tint = CharcoalBlack)
                            }
                            IconButton(
                                onClick = { isFrontCamera = !isFrontCamera },
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(CharcoalBlack.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "Flip Camera", tint = White)
                            }
                        }
                    } else {
                        // Demo mode - no camera available
                        Box(modifier = Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)).background(CharcoalBlack.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = WarmGrey, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Camera not available", color = WarmGrey, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.runDemoAnalysis() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Run Demo Analysis", color = CharcoalBlack)
                                }
                            }
                        }
                    }
                }
                is AnemiaUiState.Analyzing -> {
                    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TurmericGold)
                    }
                    Text("Analyzing LAB Color Space...", color = White)
                }
                is AnemiaUiState.Result -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!hasCamera) { DemoModeBadge(); Spacer(modifier = Modifier.height(8.dp)) }
                            Text("Conjunctival Pallor:", color = WarmGrey)
                            Text(state.severity.label, color = CharcoalBlack, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Estimated Hb Range:", color = WarmGrey)
                            Text(state.hbRange, color = if (state.severity == ConjunctivalPallorAnalyzer.Result.Severity.NORMAL) ForestGreen else CoralRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Confidence: ${(state.confidence * 100).toInt()}%", color = WarmGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is AnemiaUiState.Error -> {
                    Text(state.message, color = CoralRed, textAlign = TextAlign.Center)
                    if (!hasCamera) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.runDemoAnalysis() }, colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)) {
                            Text("Try Demo Analysis", color = CharcoalBlack)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState !is AnemiaUiState.Idle) {
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGrey)
                ) { Text("Retake Photo", color = White) }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// RPPG VITALS
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RppgHeartRateScreen(
    onNavigateBack: () -> Unit,
    viewModel: RppgViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera = rememberHasCamera()
    val cameraProviderFuture = remember { if (hasCamera) ProcessCameraProvider.getInstance(context) else null }
    val executor = remember { Executors.newSingleThreadExecutor() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.contactlessVitals) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack, titleContentColor = White, navigationIconContentColor = White)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasCamera) {
                DemoModeBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text("Hold phone steady showing patient's face in frame", color = White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is RppgUiState.Idle, is RppgUiState.Capturing -> {
                    if (hasCamera && cameraProviderFuture != null) {
                        Box(modifier = Modifier.size(300.dp)) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        imageAnalysis.setAnalyzer(executor) { image ->
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            bitmap?.let {
                                                if (viewModel.uiState.value is RppgUiState.Capturing) {
                                                    viewModel.processFrame(it, System.currentTimeMillis())
                                                }
                                            }
                                        }
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                                    } catch (e: Exception) { Log.e("Camera", "Binding failed", e) }
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                            )
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(color = TurmericGold, topLeft = Offset(size.width*0.1f, size.height*0.2f), size = androidx.compose.ui.geometry.Size(size.width*0.8f, size.height*0.6f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                            }
                            if (state is RppgUiState.Idle) {
                                Button(
                                    onClick = { viewModel.startCapture() },
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                                ) { Text("Start 15s Capture", color = CharcoalBlack) }
                            } else if (state is RppgUiState.Capturing) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                                    color = TurmericGold, trackColor = CharcoalBlack.copy(alpha=0.5f)
                                )
                            }
                        }
                    } else {
                        // Demo mode - no camera
                        Box(modifier = Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)).background(CharcoalBlack.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = WarmGrey, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Camera not available", color = WarmGrey, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                if (state is RppgUiState.Idle) {
                                    Button(
                                        onClick = { viewModel.runDemoCapture() },
                                        colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Run Demo Capture", color = CharcoalBlack)
                                    }
                                } else if (state is RppgUiState.Capturing) {
                                    Text("Simulating capture...", color = TurmericGold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth(0.8f),
                                        color = TurmericGold, trackColor = CharcoalBlack.copy(alpha=0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                is RppgUiState.Processing -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(40.dp)) {
                        CircularProgressIndicator(color = TurmericGold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Extracting rPPG Signal...", color = White)
                        Text("Applying FFT and Filters...", color = WarmGrey)
                    }
                }
                is RppgUiState.Result -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!hasCamera) { DemoModeBadge(); Spacer(modifier = Modifier.height(8.dp)) }
                            Text("Heart Rate (rPPG):", color = WarmGrey)
                            Text("${state.heartRate} BPM", color = ForestGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Signal Confidence: ${(state.confidence * 100).toInt()}%", color = WarmGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                is RppgUiState.Error -> {
                    Text(state.message, color = CoralRed, textAlign = TextAlign.Center)
                    if (!hasCamera) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.runDemoCapture() }, colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)) {
                            Text("Try Demo Capture", color = CharcoalBlack)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState is RppgUiState.Result || uiState is RppgUiState.Error) {
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGrey)
                ) { Text("Restart", color = White) }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// CAPILLARY REFILL
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapillaryRefillScreen(
    onNavigateBack: () -> Unit,
    viewModel: RppgViewModel = hiltViewModel()
) {
    val strings = LocalStrings.current
    val crtState by viewModel.crtState.collectAsState()
    val crtResult by viewModel.crtResult.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera = rememberHasCamera()
    val cameraProviderFuture = remember { if (hasCamera) ProcessCameraProvider.getInstance(context) else null }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.capillaryRefillTest) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalBlack, titleContentColor = White, navigationIconContentColor = White)
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasCamera) {
                DemoModeBadge()
                Spacer(modifier = Modifier.height(8.dp))
            }

            val instruction = when (crtState) {
                CapillaryRefillProcessor.State.IDLE -> "Press fingernail firmly until it turns pale."
                CapillaryRefillProcessor.State.BLANCHED -> "Hold pressure. Then release suddenly."
                CapillaryRefillProcessor.State.REFILLING -> "Refilling! Keep camera steady."
                CapillaryRefillProcessor.State.COMPLETE -> "Test complete."
            }
            Text(instruction, color = TurmericGold, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (crtState != CapillaryRefillProcessor.State.COMPLETE) {
                if (hasCamera && cameraProviderFuture != null) {
                    Box(modifier = Modifier.size(300.dp)) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                    val imageAnalysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                                    imageAnalysis.setAnalyzer(executor) { image ->
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        bitmap?.let { viewModel.processCapillaryFrame(it, System.currentTimeMillis()) }
                                    }
                                    imageCapture = ImageCapture.Builder().build()
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis, imageCapture)
                                } catch (e: Exception) { Log.e("Camera", "Binding failed", e) }
                                previewView
                            },
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(color = if (crtState == CapillaryRefillProcessor.State.BLANCHED) CoralRed else TurmericGold, topLeft = Offset(size.width*0.35f, size.height*0.35f), size = androidx.compose.ui.geometry.Size(size.width*0.3f, size.height*0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                        }
                        IconButton(
                            onClick = {
                                val currentImageCapture = imageCapture ?: return@IconButton
                                currentImageCapture.takePicture(
                                    executor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bitmap = imageProxyToBitmap(image)
                                            image.close()
                                            bitmap?.let { b ->
                                                coroutineScope.launch {
                                                    com.glucodes.swarmdoc.util.ImageUtils.saveBitmapToDevice(context, b, "CapillaryRefill_${System.currentTimeMillis()}.jpg")
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp).background(TurmericGold, CircleShape)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture Manual Photo", tint = CharcoalBlack)
                        }
                    }
                } else {
                    // Demo mode - no camera
                    Box(modifier = Modifier.size(300.dp).clip(RoundedCornerShape(16.dp)).background(CharcoalBlack.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = WarmGrey, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Camera not available", color = WarmGrey, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (crtState == CapillaryRefillProcessor.State.IDLE) {
                                Button(
                                    onClick = { viewModel.runDemoCapillary() },
                                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = CharcoalBlack)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Run Demo Test", color = CharcoalBlack)
                                }
                            } else {
                                Text("Simulating...", color = TurmericGold, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                CircularProgressIndicator(color = TurmericGold, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            } else {
                crtResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top=32.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (!hasCamera) { DemoModeBadge(); Spacer(modifier = Modifier.height(8.dp)) }
                            Text("Refill Time:", color = WarmGrey)
                            Text("${res.refillTimeMs / 1000f} sec", color = CharcoalBlack, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Status:", color = WarmGrey)
                            Text(res.status, color = if (res.status == "Normal") ForestGreen else CoralRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AI Confidence: ${(res.confidence * 100).toInt()}%", color = WarmGrey, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (crtState == CapillaryRefillProcessor.State.COMPLETE) {
                Button(
                    onClick = { viewModel.resetCapillary() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmGrey)
                ) { Text("Restart Test", color = White) }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------------------------------
/**
 * Convert ImageProxy to Bitmap.
 * Handles both JPEG and YUV_420_888 formats from CameraX.
 */
fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val format = image.format
        if (format == ImageFormat.JPEG) {
            // JPEG: simple decode
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } else if (format == ImageFormat.YUV_420_888) {
            // YUV_420_888: convert to NV21 then to JPEG then decode
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
            // Handle rotation
            val rotation = image.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix()
                matrix.postRotate(rotation.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } else {
            // Fallback: try raw decode
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (e: Exception) {
        Log.e("ImageProxy", "Failed to convert: ${e.message}")
        null
    }
}
