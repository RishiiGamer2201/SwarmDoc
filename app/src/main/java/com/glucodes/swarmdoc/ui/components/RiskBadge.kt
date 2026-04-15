package com.glucodes.swarmdoc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.glucodes.swarmdoc.ui.theme.*

@Composable
fun RiskBadge(
    riskLevel: String,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = true,
) {
    val (bgColor, textColor, label, description) = when (riskLevel.uppercase()) {
        "EMERGENCY" -> listOf(CoralRed, White, "EMERGENCY — Refer Immediately", "Emergency risk level")
        "URGENT" -> listOf(WarmAmber, CharcoalBlack, "URGENT — Medical Consultation Needed", "Urgent risk level")
        else -> listOf(SageGreen, White, "STABLE — Home Management", "Normal risk level")
    }

    val isEmergency = riskLevel.uppercase() == "EMERGENCY"

    // Pulse animation for emergency
    val infiniteTransition = rememberInfiniteTransition(label = "riskPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEmergency) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isEmergency) 0.85f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(80.dp)
            .scale(pulseScale)
            .semantics { contentDescription = description as String },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = (bgColor as Color).copy(alpha = pulseAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEmergency) 12.dp else 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (isEmergency) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = textColor as Color,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label as String,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor as Color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun RiskBadgeSmall(
    riskLevel: String,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (riskLevel.uppercase()) {
        "EMERGENCY" -> Pair(CoralRed, "Emergency")
        "URGENT" -> Pair(WarmAmber, "Urgent")
        else -> Pair(SageGreen, "Normal")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (riskLevel.uppercase() == "URGENT") CharcoalBlack else White,
            fontWeight = FontWeight.Bold,
        )
    }
}
