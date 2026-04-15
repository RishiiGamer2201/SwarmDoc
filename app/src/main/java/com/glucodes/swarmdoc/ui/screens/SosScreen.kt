package com.glucodes.swarmdoc.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.SosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SosViewModel = hiltViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopAppBar(
            title = { Text(strings.emergencyAlerts, fontWeight = FontWeight.Bold, color = CoralRed) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.phase) {
                SosPhase.IDLE -> SosIdleView(viewModel)
                SosPhase.CONFIRMING -> SosConfirmView(viewModel)
                SosPhase.ALERTING -> SosAlertingView()
                SosPhase.RESULT -> SosResultView(state, viewModel, context)
            }
        }
    }
}

@Composable
private fun SosIdleView(viewModel: SosViewModel) {
    Spacer(modifier = Modifier.height(60.dp))

    val anim = rememberInfiniteTransition(label = "sos_pulse")
    val scale by anim.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((130 * scale).dp)
                .clip(CircleShape)
                .background(CoralRed.copy(alpha = 0.1f))
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(CoralRed)
                .clickable { viewModel.startSos() },
            contentAlignment = Alignment.Center,
        ) {
            Text("SOS", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = White)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    Text(
        "For patient emergencies only",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = CharcoalBlack,
        textAlign = TextAlign.Center,
    )
    Text(
        "This will alert the nearest available doctor.",
        style = MaterialTheme.typography.bodyMedium,
        color = WarmGrey,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))
    TextButton(onClick = { /* non-emergency referral form */ }) {
        Text("Non-emergency referral", color = ForestGreen)
    }
}

@Composable
private fun SosConfirmView(viewModel: SosViewModel) {
    Spacer(modifier = Modifier.height(80.dp))
    Icon(Icons.Rounded.Warning, contentDescription = null, tint = CoralRed, modifier = Modifier.size(64.dp))
    Spacer(modifier = Modifier.height(24.dp))
    Text("Confirm Emergency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CoralRed, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    Text("This will alert the nearest doctor to your patient's location.", style = MaterialTheme.typography.bodyLarge, color = WarmGrey, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { viewModel.confirmSos() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text("Yes, Alert Doctor", fontWeight = FontWeight.Bold, color = White, fontSize = 16.sp)
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = { viewModel.cancelSos() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text("Cancel", fontWeight = FontWeight.Bold, color = WarmGrey)
    }
}

@Composable
private fun SosAlertingView() {
    Spacer(modifier = Modifier.height(80.dp))

    val anim = rememberInfiniteTransition(label = "alerting")
    val ringSize by anim.animateFloat(
        initialValue = 60f, targetValue = 120f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "ring"
    )
    val ringAlpha by anim.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(ringSize.dp).clip(CircleShape).background(CoralRed.copy(alpha = ringAlpha)))
        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(CoralRed), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = White, modifier = Modifier.size(32.dp))
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    Text("Alerting nearest doctor...", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CoralRed, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    CircularProgressIndicator(color = CoralRed)
}

@Composable
private fun SosResultView(
    state: SosUiState,
    viewModel: SosViewModel,
    context: android.content.Context,
) {
    Spacer(modifier = Modifier.height(40.dp))

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Doctor Alerted", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ForestGreen, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(state.assignedDoctorName, fontWeight = FontWeight.Bold, color = CharcoalBlack, style = MaterialTheme.typography.titleLarge)
                Text("at ${state.assignedPhcName}", color = WarmGrey)
                Spacer(modifier = Modifier.height(12.dp))
                Text(state.resultMessage, color = CharcoalBlack, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Send WhatsApp message
        Button(
            onClick = {
                val msg = "EMERGENCY ALERT from SwarmDoc\nDoctor: ${state.assignedDoctorName}\nRisk: ${state.patientRiskLevel}\nLocation: Puri District\nPlease respond immediately."
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, msg)
                    setPackage("com.whatsapp")
                }
                try { context.startActivity(intent) } catch (_: Exception) {
                    val fallback = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, msg)
                    }
                    context.startActivity(Intent.createChooser(fallback, "Send alert"))
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.Chat, contentDescription = null, tint = White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send WhatsApp to Doctor", fontWeight = FontWeight.Bold, color = White)
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.resetSos() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Done", fontWeight = FontWeight.Bold, color = ForestGreen)
        }
    }
}

enum class SosPhase { IDLE, CONFIRMING, ALERTING, RESULT }

data class SosUiState(
    val phase: SosPhase = SosPhase.IDLE,
    val assignedDoctorName: String = "",
    val assignedDoctorPhone: String = "",
    val assignedPhcName: String = "",
    val resultMessage: String = "",
    val patientRiskLevel: String = "",
    val wasReassigned: Boolean = false,
)
