package com.glucodes.swarmdoc.ui.screens

import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.domain.models.InputMode
import com.glucodes.swarmdoc.ui.components.ShimmerEffect
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.IntakeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoInputScreen(
    onNavigateToDiagnosis: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: IntakeViewModel = hiltViewModel(),
) {
    var selectedMode by remember { mutableStateOf(0) } // 0: Wound, 1: Eye, 2: Report
    val modes = listOf("Wound / Skin", "Eye Exam", "Paper Report")
    
    var captured by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    val hasCamera = rememberHasCamera()
    
    val savedId by viewModel.savedConsultationId.collectAsState()

    LaunchedEffect(savedId) {
        if (savedId != null) {
            onNavigateToDiagnosis(savedId!!)
            viewModel.resetState()
        }
    }

    LaunchedEffect(analyzing) {
        if (analyzing) {
            delay(1500) // Simulate ONNX inference time
            
            // Map fake result to consultation creation depending on mode
            val symptoms = when (selectedMode) {
                0 -> listOf("rash", "itching", "swelling")
                1 -> listOf("eye_discharge")
                else -> listOf("weakness", "fever")
            }
            
            viewModel.saveIntakeAndProceed(
                name = "", age = 30, sex = "MALE", village = "Unknown", district = "Jaipur",
                selectedSymptoms = symptoms,
                additionalSymptoms = "Scanned via photo mode",
                durationDays = 3, temperature = null, recentTravel = false,
                inputMode = InputMode.PHOTO
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Analysis") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalBlack,
                    titleContentColor = White,
                    navigationIconContentColor = White,
                ),
            )
        },
        containerColor = CharcoalBlack
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!captured) {
                // Camera Preview or Demo Mode
                if (hasCamera) {
                    CameraPreviewStub(modifier = Modifier.fillMaxSize())
                } else {
                    // Demo mode - no camera
                    Box(
                        modifier = Modifier.fillMaxSize().background(CharcoalBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DemoModeBadge()
                            Spacer(modifier = Modifier.height(24.dp))
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = WarmGrey, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Camera not available on this device", color = WarmGrey, textAlign = TextAlign.Center)
                            Text("Using demo capture mode", color = WarmGrey.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                // Mode selector at top
                ScrollableTabRow(
                    selectedTabIndex = selectedMode,
                    containerColor = CharcoalBlack.copy(alpha = 0.5f),
                    contentColor = White,
                    edgePadding = 16.dp,
                    indicator = { },
                    divider = { }
                ) {
                    modes.forEachIndexed { index, title ->
                        val selected = selectedMode == index
                        Tab(
                            selected = selected,
                            onClick = { selectedMode = index },
                            text = { 
                                Text(
                                    title, 
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) TurmericGold else White.copy(0.7f)
                                ) 
                            }
                        )
                    }
                }

                // Overlay Guide based on mode
                if (hasCamera) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val guideShape = when (selectedMode) {
                            0 -> RoundedCornerShape(16.dp)
                            1 -> CircleShape
                            else -> RoundedCornerShape(4.dp)
                        }
                        val guideSize = when (selectedMode) {
                            0 -> Modifier.size(250.dp)
                            1 -> Modifier.size(280.dp, 120.dp)
                            else -> Modifier.size(280.dp, 400.dp)
                        }
                        
                        Box(
                            modifier = guideSize
                                .border(2.dp, TurmericGoldLayer, guideShape)
                        )
                    }
                }

                // Capture Button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                        .size(80.dp)
                        .border(4.dp, White, CircleShape)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable { captured = true },
                )
            } else {
                // Captured Review Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Parchment)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (!hasCamera) {
                            DemoModeBadge()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Stub for captured image
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(LightGrey),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = White, modifier = Modifier.size(48.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (analyzing) {
                            Text("Analyzing image with MobileNetV3...", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            ShimmerEffect(height = 24.dp, cornerRadius = 4.dp, modifier = Modifier.fillMaxWidth(0.6f))
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerEffect(height = 16.dp, cornerRadius = 4.dp, modifier = Modifier.fillMaxWidth(0.8f))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { captured = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WarmGrey)
                                ) {
                                    Text("Retake")
                                }
                                
                                Button(
                                    onClick = { analyzing = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TurmericGold)
                                ) {
                                    Text("Analyze", color = CharcoalBlack)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Temporary color for overlay outline
val TurmericGoldLayer = TurmericGold.copy(alpha = 0.8f)

@Composable
fun CameraPreviewStub(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCamera = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    if (!hasCamera) {
        // Fallback when no camera hardware
        Box(
            modifier = modifier.background(CharcoalBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = WarmGrey, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera not available", color = WarmGrey, textAlign = TextAlign.Center)
            }
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Camera binding failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = modifier
    )
}
