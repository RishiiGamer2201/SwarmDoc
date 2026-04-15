package com.glucodes.swarmdoc.ui.screens

import android.view.SoundEffectConstants
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    privacyMode: Boolean,
    onNavigateToConsultation: () -> Unit,
    onNavigateToPatients: () -> Unit,
    onNavigateToCommunity: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onPrivacyModeChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val workerName by viewModel.workerName.collectAsState()
    val patientCount by viewModel.patientCount.collectAsState()
    val activeWorkers by viewModel.activeWorkers.collectAsState()
    val hasOutbreakAlert by viewModel.hasOutbreakAlert.collectAsState()
    val focusArea by viewModel.focusArea.collectAsState()
    val view = LocalView.current

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000), repeatMode = RepeatMode.Reverse
        ), label = "pulseAlpha"
    )

    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (privacyMode) PrivacySurface else Parchment)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        strings.swarmDoc,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (privacyMode) PrivacyIndigo else ForestGreen,
                    )
                    Text(
                        workerName,
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGrey,
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onNavigateToInventory()
                }) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TurmericGold),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalHospital,
                            contentDescription = "Medicine Inventory",
                            tint = CharcoalBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (privacyMode) PrivacySurface else Parchment,
            ),
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Community Health Pulse Strip (48dp)
            val stripColor = if (hasOutbreakAlert) CoralRed else ForestGreen
            val stripAlpha = if (hasOutbreakAlert) pulseAlpha else 1f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(stripColor.copy(alpha = stripAlpha * 0.15f))
                    .clickable {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onNavigateToCommunity()
                    }
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stripColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (hasOutbreakAlert) strings.outbreakAlert
                           else "$activeWorkers ${strings.workersActiveNearYou}",
                    style = MaterialTheme.typography.labelMedium,
                    color = stripColor,
                    fontWeight = if (hasOutbreakAlert) FontWeight.Bold else FontWeight.Normal,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Card 1: Start Consultation (160dp, primary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clickable {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateToConsultation()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (privacyMode) PrivacyIndigo else ForestGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                strings.startConsultation,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = TurmericGold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                strings.tapToBeginAssessment,
                                style = MaterialTheme.typography.bodyMedium,
                                color = White.copy(alpha = 0.8f),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Icon(Icons.Rounded.Keyboard, contentDescription = null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Card 1.5: Diagnostics
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateToDiagnostics()
                        },
                    colors = CardDefaults.cardColors(containerColor = TurmericGold),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Memory, contentDescription = null, tint = CharcoalBlack, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.diagnostics, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                            Text(strings.onDeviceVitals, style = MaterialTheme.typography.bodySmall, color = CharcoalBlack.copy(alpha=0.8f))
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = CharcoalBlack)
                    }
                }

                // Card 2: My Patients (100dp, secondary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateToPatients()
                        },
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ParchmentDark),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.People, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(strings.myPatients, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                            Text("$patientCount ${strings.patientsLogged}", style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WarmGrey)
                    }
                }

                // Community Map Banner (80dp)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clickable {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onNavigateToCommunity()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasOutbreakAlert) CoralRed.copy(alpha = 0.1f) else ForestGreen.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Hub, contentDescription = null,
                            tint = if (hasOutbreakAlert) CoralRed else ForestGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.communityHealthMap,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalBlack
                            )
                            Text(
                                if (hasOutbreakAlert) strings.outbreakAlert else strings.allClearInZone,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasOutbreakAlert) CoralRed else SageGreen,
                            )
                        }
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = WarmGrey)
                    }
                }

                // Focus Area Section — expanded with Do's, Don'ts, and Procedures
                if (focusArea != "General Health") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.todaysFocus, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = WarmGrey)
                    Spacer(modifier = Modifier.height(8.dp))

                    val guidance = getFocusGuidance(focusArea)

                    // Quick tiles
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        guidance.tiles.take(2).forEach { tile ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable {
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        onNavigateToConsultation()
                                    },
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(tile.first, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    Text(tile.second, style = MaterialTheme.typography.labelSmall, color = WarmGrey)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Do's Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SageGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SageGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.dos, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ForestGreen)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            guidance.dos.forEach { item ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("✓ ", color = SageGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(item, style = MaterialTheme.typography.bodySmall, color = CharcoalBlack)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Don'ts Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CoralRed.copy(alpha = 0.06f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Cancel, contentDescription = null, tint = CoralRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.donts, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CoralRed)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            guidance.donts.forEach { item ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("✗ ", color = CoralRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(item, style = MaterialTheme.typography.bodySmall, color = CharcoalBlack)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Basic Procedures Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TurmericGold.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = TurmericGold, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.basicProcedures, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CharcoalBlack)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            guidance.procedures.forEachIndexed { index, (title, description) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text("${index + 1}. $title", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = ForestGreen)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = WarmGrey)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacer for scrolling
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Structured guidance data for each focus area
data class FocusGuidance(
    val tiles: List<Pair<String, String>>,
    val dos: List<String>,
    val donts: List<String>,
    val procedures: List<Pair<String, String>>,
)

private fun getFocusGuidance(focusArea: String): FocusGuidance = when (focusArea) {
    "Maternal and Child Health" -> FocusGuidance(
        tiles = listOf("ANC Check" to "Antenatal care", "Immunization" to "Child tracker"),
        dos = listOf(
            "Ensure all pregnant women receive at least 4 ANC visits",
            "Track IFA (Iron Folic Acid) tablet consumption daily",
            "Weigh the mother at every visit and record weight gain",
            "Encourage institutional delivery at the nearest PHC",
            "Ensure complete immunization schedule (BCG, OPV, DPT, Measles)",
            "Promote exclusive breastfeeding for first 6 months",
            "Monitor for danger signs: severe headache, blurred vision, swelling of feet",
            "Ensure TT (Tetanus Toxoid) vaccination during pregnancy",
        ),
        donts = listOf(
            "Do NOT ignore vaginal bleeding at any stage of pregnancy",
            "Do NOT allow traditional medicines without medical advice",
            "Do NOT delay referral if blood pressure is above 140/90",
            "Do NOT skip recording the date of last menstrual period (LMP)",
            "Do NOT advise fasting or dietary restriction during pregnancy",
            "Do NOT ignore reduced fetal movement — refer immediately",
        ),
        procedures = listOf(
            "Blood Pressure Measurement" to "Wrap the cuff 2cm above the elbow crease. Inflate until pulse disappears. Deflate slowly. Record systolic (first sound) and diastolic (sound stops). Normal: 120/80.",
            "Fundal Height Measurement" to "Ask the mother to lie flat. Using a tape measure, measure from the pubic bone to the top of the uterus (fundus). After 20 weeks, fundal height in cm ≈ weeks of gestation.",
            "MUAC Measurement (Child)" to "Place the MUAC tape at the midpoint of the left upper arm. Read the color: Green (>13.5cm) = healthy, Yellow (12.5-13.5cm) = moderate malnutrition, Red (<12.5cm) = severe acute malnutrition — refer immediately.",
            "Weight-for-Age Plotting" to "Weigh the child using a spring scale. Plot on the growth chart. If weight falls below -2 SD, the child is underweight. Below -3 SD is severely underweight — refer to NRC.",
        ),
    )
    "Elderly Care" -> FocusGuidance(
        tiles = listOf("Elder Vitals" to "BP, Sugar check", "Fall Risk" to "Assessment"),
        dos = listOf(
            "Check blood pressure and blood sugar at every visit",
            "Ensure the elderly person is taking prescribed medicines regularly",
            "Check for signs of dehydration: dry lips, sunken eyes, skin turgor",
            "Ask about any falls in the last month",
            "Encourage daily light physical activity (walking, stretching)",
            "Screen for vision and hearing problems annually",
            "Ensure proper nutrition with adequate protein intake",
            "Monitor for signs of depression: loss of interest, not eating, isolation",
        ),
        donts = listOf(
            "Do NOT ignore confusion or sudden change in behaviour — could be stroke",
            "Do NOT let elderly patients self-adjust medication doses",
            "Do NOT dismiss chest pain or breathlessness — refer immediately",
            "Do NOT ignore repeated falls — assess for neurological issues",
            "Do NOT allow polypharmacy without doctor review",
            "Do NOT skip asking about urinary incontinence — it affects quality of life",
        ),
        procedures = listOf(
            "Blood Pressure Monitoring" to "Seat patient comfortably for 5 minutes before measuring. Use properly sized cuff. Take 2 readings, 1 minute apart. Record average. Hypertension: >140/90 on two occasions.",
            "Blood Sugar Check (Glucometer)" to "Clean fingertip with alcohol swab. Prick side of fingertip. Apply drop to test strip. Wait for reading. Fasting: 70-100 mg/dL normal. Post-meal: <140 mg/dL normal. >200 mg/dL = refer.",
            "Fall Risk Assessment" to "Ask: Have you fallen in the last 6 months? Can you stand from a chair without using hands? Can you walk 3 meters and return? If NO to any → high fall risk. Remove home hazards, recommend walking aid.",
            "Pill Box Audit" to "Count remaining pills vs. expected count. If discrepancy > 2 days, counsel on adherence. For complex regimens, organize pills in a daily pill box with AM/PM compartments.",
        ),
    )
    "Skin and Dermatology" -> FocusGuidance(
        tiles = listOf("Skin Scan" to "Photo analysis", "Wound Care" to "Treatment guide"),
        dos = listOf(
            "Always photograph skin lesions with a ruler for size reference",
            "Clean wounds with normal saline before assessment",
            "Use clean gloves for all wound examinations",
            "Document size, color, borders, and discharge of all lesions",
            "Ask about duration: how long has the patient had this?",
            "Check for fever with any skin infection",
            "Screen all household contacts if scabies or fungal infection suspected",
            "Apply topical medication as per protocol and cover with clean dressing",
        ),
        donts = listOf(
            "Do NOT apply turmeric or traditional pastes on open wounds",
            "Do NOT ignore non-healing ulcers lasting >2 weeks — could be malignancy",
            "Do NOT burst blisters — they are a natural barrier",
            "Do NOT use expired topical creams",
            "Do NOT ignore a mole that has changed shape, size, or color",
            "Do NOT use same dressing material on multiple patients",
        ),
        procedures = listOf(
            "Wound Cleaning" to "Wear clean gloves. Irrigate wound with normal saline (not water). Gently remove debris. Pat dry with sterile gauze. Apply prescribed ointment. Cover with clean dressing. Change dressing daily.",
            "Lesion Photography" to "Use flash on. Place a ruler next to lesion. Take photo from 30cm distance directly above. Take a second photo from an angle. Include surrounding skin. Note location on body in patient record.",
            "Scabies Treatment" to "Apply Permethrin 5% cream from neck to toes at bedtime. Wash off after 8-12 hours. Repeat after 1 week. Treat ALL household members simultaneously. Wash all clothes and bedding in hot water.",
            "Ring Worm Treatment" to "Apply Clotrimazole cream twice daily on and 2cm around the lesion. Continue for 2 weeks after lesion clears. Keep the area dry. If >3 lesions or scalp affected, refer for oral antifungal.",
        ),
    )
    "Respiratory and TB" -> FocusGuidance(
        tiles = listOf("Cough Screener" to "Acoustic analysis", "TB Check" to "Symptom screening"),
        dos = listOf(
            "Ask every patient with cough: How many days/weeks has the cough lasted?",
            "Screen for TB in anyone with cough >2 weeks, weight loss, night sweats, or fever",
            "Ensure sputum collection containers are available",
            "Advise cough etiquette: cover mouth, wash hands, use masks",
            "Track all TB patients' DOTS medication daily",
            "Ensure ventilation in the patient's living area",
            "Follow up with sputum-positive contacts — screen household members",
            "Record peak flow readings for asthma patients if meter available",
        ),
        donts = listOf(
            "Do NOT dismiss persistent cough as 'just a cold' after 2 weeks",
            "Do NOT collect sputum indoors — always outdoors or in ventilated area",
            "Do NOT allow TB patients to skip DOTS medication even for one day",
            "Do NOT ignore hemoptysis (blood in sputum) — refer immediately",
            "Do NOT send suspected TB patients home without sputum collection plan",
            "Do NOT use cough suppressants to mask TB symptoms",
        ),
        procedures = listOf(
            "Sputum Collection" to "Collect early morning sample. Patient should rinse mouth with water first. Take deep breath, hold, then cough deeply into container. Collect 2 samples on consecutive mornings. Label with name, date, ID. Transport upright to microscopy centre within 24 hours.",
            "DOTS Administration" to "Watch the patient swallow every single dose. Record in the TB treatment card. Intensive phase: daily for 2 months. Continuation phase: thrice weekly for 4 months. If patient misses >3 doses, report to MO.",
            "Peak Flow Measurement" to "Stand upright. Take deepest breath possible. Seal lips around mouthpiece. Blow as hard and fast as possible. Record the reading. Repeat 3 times. Record the highest value. Green zone: >80% of personal best. Yellow: 50-80%. Red: <50% — give salbutamol and refer.",
            "Contact Tracing" to "List all household members and close contacts. Screen each for: cough >2 weeks, weight loss, night sweats, fever. If any positive: collect sputum. Children <6 in contact with TB: start INH prophylaxis after ruling out active TB.",
        ),
    )
    "Nutrition and Anemia" -> FocusGuidance(
        tiles = listOf("Anemia Eye" to "Pallor check", "MUAC Measure" to "Nutrition status"),
        dos = listOf(
            "Check conjunctival pallor in all women and children",
            "Measure MUAC for all children 6 months to 5 years",
            "Promote iron-rich foods: green leafy vegetables, jaggery, dates, eggs, liver",
            "Ensure IFA tablets are taken with Vitamin C (lemon water) for better absorption",
            "Weigh all children monthly and plot on growth chart",
            "Counsel on complementary feeding after 6 months: thick dal, mashed banana, egg",
            "Deworm all children 1-5 years every 6 months with Albendazole",
            "Promote kitchen gardens for fresh vegetable access",
        ),
        donts = listOf(
            "Do NOT give IFA tablets with tea, milk, or coffee — it blocks absorption",
            "Do NOT ignore severe pallor — refer for hemoglobin test immediately",
            "Do NOT allow only rice-water as complementary food — it lacks nutrition",
            "Do NOT dismiss growth faltering — 2 consecutive weight drops need action",
            "Do NOT give iron supplements to a child with active diarrhea",
            "Do NOT skip deworming — worm infestations are a major cause of anemia",
        ),
        procedures = listOf(
            "Conjunctival Pallor Check" to "Gently pull down the lower eyelid. Observe the inner surface. Bright red = normal hemoglobin. Pale pink = mild anemia (Hb 9-11). Very pale/white = severe anemia (Hb <7) — refer immediately. Check both eyes.",
            "MUAC Measurement" to "Use left arm. Find midpoint between shoulder and elbow. Wrap MUAC tape snugly (not tight). Read color and measurement. Green >13.5cm: OK. Yellow 12.5-13.5cm: Moderate Acute Malnutrition — counsel + supplement. Red <12.5cm: Severe — refer to NRC immediately.",
            "IFA Supplementation" to "Pregnant women: 1 IFA tablet daily (60mg iron + 0.5mg folic acid) for minimum 180 days. Adolescent girls: 1 tablet weekly. Children 6-59 months: Iron syrup 20mg/day. Take 1 hour before or 2 hours after meals. Side effects (nausea, black stool) are normal — reassure.",
            "Growth Monitoring" to "Weigh child undressed on calibrated scale. Plot on WHO growth chart. Check trend: upward = healthy. Flat = growth faltering — counsel on feeding. Downward = danger — refer. Z-score below -2: underweight. Below -3: severely underweight with medical emergency if edema present.",
        ),
    )
    else -> FocusGuidance(
        tiles = emptyList(),
        dos = emptyList(),
        donts = emptyList(),
        procedures = emptyList(),
    )
}
