package com.glucodes.swarmdoc.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingWalkthroughScreen(
    onComplete: () -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    val strings = LocalStrings.current
    val pagerState = rememberPagerState(pageCount = { 6 })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().background(Parchment)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> WelcomeCard()
                1 -> ConsultationCard()
                2 -> RiskLevelsCard()
                3 -> PatientProfileCard()
                4 -> CommunityRadarCard()
                5 -> SosCard()
            }
        }

        // Back button top-left
        IconButton(
            onClick = {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                } else {
                    onNavigateBack()
                }
            },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
        }

        // Skip button top-right
        if (pagerState.currentPage < 5) {
            TextButton(
                onClick = { onComplete() },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) {
                Text(strings.skip, color = WarmGrey, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom navigation
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back arrow
            if (pagerState.currentPage > 0) {
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (idx == pagerState.currentPage) ForestGreen else WarmGrey.copy(alpha = 0.3f))
                    )
                }
            }

            // Next arrow or Get Started
            if (pagerState.currentPage < 5) {
                IconButton(onClick = {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Icon(Icons.Rounded.ArrowForward, contentDescription = "Next", tint = ForestGreen)
                }
            } else {
                Button(
                    onClick = { onComplete() },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(strings.getStarted, fontWeight = FontWeight.Bold, color = White)
                }
            }
        }
    }
}

@Composable
private fun WelcomeCard() {
    val anim = rememberInfiniteTransition(label = "ripple")
    val rippleAlpha by anim.animateFloat(
        initialValue = 0.1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "ripple"
    )

    OnboardingCard(
        icon = Icons.Rounded.HealthAndSafety,
        iconBackgroundColor = ForestGreen.copy(alpha = rippleAlpha),
        title = "SwarmDoc",
        subtitle = "Made for ASHA Workers",
        description = "This app works fully offline. No internet needed for diagnosis.",
    )
}

@Composable
private fun ConsultationCard() {
    OnboardingCard(
        icon = Icons.Rounded.TouchApp,
        iconBackgroundColor = TurmericGold.copy(alpha = 0.2f),
        title = "Starting a Consultation",
        subtitle = "",
        description = "Tap Start Consultation to begin. You can describe symptoms by voice, text, or photo. The app guides you through the full process step by step.",
    )
}

@Composable
private fun RiskLevelsCard() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Risk Levels", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("After diagnosis, you will always see one of these three levels and exactly what to do next.",
            style = MaterialTheme.typography.bodyMedium, color = WarmGrey, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        RiskRow(CoralRed, "Emergency", "Refer to PHC immediately.")
        Spacer(modifier = Modifier.height(16.dp))
        RiskRow(AmberOrange, "Urgent", "Visit within 24 hours.")
        Spacer(modifier = Modifier.height(16.dp))
        RiskRow(SageGreen, "Stable", "Home care guidance provided.")
    }
}

@Composable
private fun RiskRow(color: Color, label: String, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(16.dp),
    ) {
        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
            Text(description, color = CharcoalBlack, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PatientProfileCard() {
    OnboardingCard(
        icon = Icons.Rounded.Person,
        iconBackgroundColor = ForestGreen.copy(alpha = 0.15f),
        title = "Your Patient Profile",
        subtitle = "",
        description = "Tap the profile icon to see all your patient's history, reports, and captured photos. Everything a doctor needs is in one place.",
    )
}

@Composable
private fun CommunityRadarCard() {
    OnboardingCard(
        icon = Icons.Rounded.Hub,
        iconBackgroundColor = TurmericGold.copy(alpha = 0.2f),
        title = "Community Health Radar",
        subtitle = "",
        description = "When you meet other ASHA workers, the app syncs symptom data over Bluetooth. If cases cluster in your area, you get an outbreak warning automatically.",
    )
}

@Composable
private fun SosCard() {
    OnboardingCard(
        icon = Icons.Rounded.NotificationsActive,
        iconBackgroundColor = CoralRed.copy(alpha = 0.15f),
        title = "SOS and Emergency Alerts",
        subtitle = "",
        description = "In an emergency, tap SOS. The nearest available doctor is alerted immediately. If all doctors are busy, the least critical case is reassigned so your patient gets help.",
    )
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(iconBackgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(56.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack, textAlign = TextAlign.Center)
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium, color = ForestGreen, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(description, style = MaterialTheme.typography.bodyLarge, color = WarmGrey, textAlign = TextAlign.Center, lineHeight = 24.sp)
    }
}
