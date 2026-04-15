package com.glucodes.swarmdoc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glucodes.swarmdoc.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLanguageSelection: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        logoVisible = true
        delay(600)
        textVisible = true
        delay(1200)
        // Always go to language selection first
        onNavigateToLanguageSelection()
    }

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "logoFade"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "textFade"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestGreen),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Animated Logo: Two hands forming circle with waveform
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center,
            ) {
                SwarmDocLogo()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "SwarmDoc",
                style = MaterialTheme.typography.displayLarge,
                color = TurmericGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "आरोग्य जाल",
                style = MaterialTheme.typography.headlineMedium,
                color = TurmericGoldLight,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(textAlpha),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Aarogya Jaal",
                style = MaterialTheme.typography.bodyLarge,
                color = White.copy(alpha = 0.7f),
                modifier = Modifier.alpha(textAlpha),
            )
        }
    }
}

@Composable
fun SwarmDocLogo(
    modifier: Modifier = Modifier,
    color: Color = TurmericGold,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier.size(160.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2 - 12

        // Outer circle (two arcs representing hands)
        drawArc(
            color = color,
            startAngle = -30f,
            sweepAngle = 200f,
            useCenter = false,
            style = Stroke(width = 6f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
        drawArc(
            color = color,
            startAngle = 150f,
            sweepAngle = 200f,
            useCenter = false,
            style = Stroke(width = 6f, cap = StrokeCap.Round),
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )

        // Inner waveform
        val wavePath = Path()
        val waveWidth = radius * 1.2f
        val waveHeight = radius * 0.3f
        val startX = center.x - waveWidth / 2
        val points = 50

        for (i in 0..points) {
            val x = startX + (waveWidth / points) * i
            val normalizedX = i.toFloat() / points * 4 * Math.PI.toFloat()
            val y = center.y + kotlin.math.sin(normalizedX + wavePhase) * waveHeight *
                    kotlin.math.sin(i.toFloat() / points * Math.PI.toFloat())

            if (i == 0) wavePath.moveTo(x, y)
            else wavePath.lineTo(x, y)
        }

        drawPath(
            path = wavePath,
            color = color,
            style = Stroke(width = 4f, cap = StrokeCap.Round),
        )

        // Small dot at center
        drawCircle(
            color = color,
            radius = 4f,
            center = center,
        )
    }
}
