package com.glucodes.swarmdoc.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated background representing a local mesh network of peers.
 */
@Composable
fun MeshNetworkAnimation(
    modifier: Modifier = Modifier,
    nodeColor: Color,
    lineColor: Color,
    activeNodes: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "meshPhase"
    )

    // Pulse for active cluster
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Define a set of nodes
        val nodes = listOf(
            Offset(width * 0.2f, height * 0.3f),
            Offset(width * 0.8f, height * 0.4f),
            Offset(width * 0.5f, height * 0.7f),
            Offset(width * 0.3f, height * 0.8f),
            Offset(width * 0.7f, height * 0.2f)
        ).mapIndexed { index, baseOffset ->
            // Add subtle floating motion to each node based on phase
            val xOffset = cos(phase + index) * 20f
            val yOffset = sin(phase + index * 1.5f) * 20f
            Offset(baseOffset.x + xOffset, baseOffset.y + yOffset)
        }

        // Draw connecting lines if nodes are relatively close
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val p1 = nodes[i]
                val p2 = nodes[j]
                val distance = Math.hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()).toFloat()
                
                if (distance < width * 0.6f) { // Connect nodes that are close
                    val alpha = (1f - (distance / (width * 0.6f))).coerceIn(0f, 0.5f)
                    val activeAlpha = if (activeNodes && (i == 0 || j == 0)) pulseAlpha else alpha
                    
                    drawLine(
                        color = lineColor.copy(alpha = activeAlpha),
                        start = p1,
                        end = p2,
                        strokeWidth = 2f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Draw the nodes
        nodes.forEachIndexed { index, offset ->
            val isCoreNode = activeNodes && index == 0
            val color = if (isCoreNode) nodeColor.copy(alpha = pulseAlpha) else nodeColor
            drawCircle(
                color = color,
                radius = if (isCoreNode) 10f else 6f,
                center = offset
            )
        }
    }
}
