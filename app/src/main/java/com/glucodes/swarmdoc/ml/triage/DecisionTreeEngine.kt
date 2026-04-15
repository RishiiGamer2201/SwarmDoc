package com.glucodes.swarmdoc.ml.triage

import android.content.Context
import com.glucodes.swarmdoc.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AgeGroup { INFANT, CHILD, ADULT, ELDER }
enum class SexType { MALE, FEMALE, OTHER }
enum class RiskLevel { EMERGENCY, URGENT, NORMAL }

data class SymptomVector(
    val ageGroup: AgeGroup,
    val age: Int,
    val sex: SexType,
    val symptomFlags: BooleanArray,
    val symptomNames: List<String>,
    val durationDays: Int,
    val temperature: Float? = null,
    val systolicBP: Int? = null,
    val diastolicBP: Int? = null,
    val pulseRate: Int? = null,
    val spo2: Int? = null,
)

data class DiagnosisResult(
    val topConditions: List<ConditionScore>,
    val riskLevel: RiskLevel,
    val recommendedAction: String,
    val communityContextFlag: Boolean = false,
)

data class ConditionScore(
    val conditionEnglish: String,
    val conditionLocal: String,
    val confidence: Float,
    val riskLevel: RiskLevel,
    val description: String,
)

data class ConditionNode(
    val condition: String,
    val conditionLocal: String,
    val requiredSymptoms: List<String>,
    val optionalSymptoms: List<String> = emptyList(),
    val baseConfidence: Float,
    val riskLevel: RiskLevel,
    val description: String,
    val minDuration: Int = 0,
    val maxDuration: Int = 365,
    // Age group risk modifiers: positive = increases risk, negative = decreases
    val ageModifiers: Map<AgeGroup, Float> = emptyMap(),
    // Sex modifiers
    val sexModifiers: Map<SexType, Float> = emptyMap(),
    // Actions by risk level
    val actionEmergency: String = "Refer to PHC immediately.",
    val actionUrgent: String = "Visit health facility within 24 hours.",
    val actionNormal: String = "Home care with follow-up in 3 days.",
)

@Singleton
class TriageInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private var conditionNodes: List<ConditionNode> = emptyList()
    private var loadTimeMs: Long = 0

    init {
        loadDecisionTree()
    }

    private fun loadDecisionTree() {
        val start = System.currentTimeMillis()
        conditionNodes = buildConditionNodes()
        loadTimeMs = System.currentTimeMillis() - start
        _isReady.value = true
    }

    suspend fun infer(input: SymptomVector): DiagnosisResult = withContext(Dispatchers.Default) {
        val start = System.nanoTime()

        val activeSymptoms = input.symptomNames.filterIndexed { i, _ ->
            i < input.symptomFlags.size && input.symptomFlags[i]
        }.toSet()

        // If symptomFlags is empty, use symptomNames directly
        val symptoms = if (activeSymptoms.isEmpty()) input.symptomNames.toSet() else activeSymptoms

        val scored = mutableListOf<ConditionScore>()

        for (node in conditionNodes) {
            val reqMatches = node.requiredSymptoms.count { it in symptoms }
            val reqTotal = node.requiredSymptoms.size
            if (reqMatches == 0) continue

            val reqRatio = reqMatches.toFloat() / reqTotal
            if (reqRatio < 0.5f) continue

            val optMatches = node.optionalSymptoms.count { it in symptoms }
            val optBonus = if (node.optionalSymptoms.isNotEmpty()) {
                optMatches.toFloat() / node.optionalSymptoms.size * 0.15f
            } else 0f

            var conf = node.baseConfidence * reqRatio + optBonus

            // Duration factor
            if (input.durationDays in node.minDuration..node.maxDuration) {
                conf += 0.05f
            } else {
                conf -= 0.10f
            }

            // Age modifier
            val ageMod = node.ageModifiers[input.ageGroup] ?: 0f
            conf += ageMod

            // Sex modifier
            val sexMod = node.sexModifiers[input.sex] ?: 0f
            conf += sexMod

            // Vitals modifiers
            if (input.temperature != null && input.temperature >= 39.5f && node.riskLevel != RiskLevel.NORMAL) {
                conf += 0.05f
            }
            if (input.spo2 != null && input.spo2 < 92) {
                conf += 0.08f
            }

            // Age-based risk escalation
            val effectiveRisk = when {
                input.ageGroup == AgeGroup.INFANT && node.riskLevel == RiskLevel.URGENT -> RiskLevel.EMERGENCY
                input.ageGroup == AgeGroup.ELDER && "breathlessness" in symptoms && node.riskLevel == RiskLevel.URGENT -> RiskLevel.EMERGENCY
                input.ageGroup == AgeGroup.INFANT && input.temperature != null && input.temperature >= 39.0f -> RiskLevel.EMERGENCY
                else -> node.riskLevel
            }

            conf = conf.coerceIn(0.1f, 0.98f)

            if (conf >= 0.3f) {
                scored.add(ConditionScore(
                    conditionEnglish = node.condition,
                    conditionLocal = node.conditionLocal,
                    confidence = conf,
                    riskLevel = effectiveRisk,
                    description = node.description,
                ))
            }
        }

        val top3 = scored.sortedByDescending { it.confidence }.take(3)

        val overallRisk = when {
            top3.isEmpty() -> RiskLevel.NORMAL
            top3.first().riskLevel == RiskLevel.EMERGENCY && top3.first().confidence >= 0.6f -> RiskLevel.EMERGENCY
            top3.first().riskLevel == RiskLevel.EMERGENCY -> RiskLevel.URGENT
            top3.first().riskLevel == RiskLevel.URGENT && top3.first().confidence >= 0.5f -> RiskLevel.URGENT
            else -> RiskLevel.NORMAL
        }

        val action = when (overallRisk) {
            RiskLevel.EMERGENCY -> top3.firstOrNull()?.let {
                conditionNodes.find { n -> n.condition == it.conditionEnglish }?.actionEmergency
            } ?: "Refer to nearest PHC immediately. Do not delay."
            RiskLevel.URGENT -> top3.firstOrNull()?.let {
                conditionNodes.find { n -> n.condition == it.conditionEnglish }?.actionUrgent
            } ?: "Visit health facility within 24 hours."
            RiskLevel.NORMAL -> top3.firstOrNull()?.let {
                conditionNodes.find { n -> n.condition == it.conditionEnglish }?.actionNormal
            } ?: "Provide home care guidance. Follow up in 3 days."
        }

        val elapsed = (System.nanoTime() - start) / 1_000_000
        android.util.Log.d("TriageEngine", "Inference completed in ${elapsed}ms, returned ${top3.size} conditions")

        DiagnosisResult(
            topConditions = top3,
            riskLevel = overallRisk,
            recommendedAction = action,
        )
    }

    private fun buildConditionNodes(): List<ConditionNode> = listOf(
        ConditionNode(
            condition = "Dengue Fever", conditionLocal = "Dengue Jwar",
            requiredSymptoms = listOf("fever", "joint_pain", "rash"),
            optionalSymptoms = listOf("headache", "vomiting", "weakness"),
            baseConfidence = 0.85f, riskLevel = RiskLevel.EMERGENCY,
            description = "High fever with joint pain and skin rash. Common during monsoon. Get blood test immediately.",
            minDuration = 2, maxDuration = 14,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.05f, AgeGroup.ELDER to 0.05f),
            actionEmergency = "Refer to PHC immediately. Start ORS. Monitor platelet count. Check for warning signs (bleeding, severe abdominal pain).",
            actionUrgent = "Visit health facility within 24 hours for blood test. Give Paracetamol for fever. Push fluids.",
        ),
        ConditionNode(
            condition = "Malaria", conditionLocal = "Malaria",
            requiredSymptoms = listOf("fever", "weakness"),
            optionalSymptoms = listOf("headache", "vomiting", "belly_pain", "joint_pain"),
            baseConfidence = 0.70f, riskLevel = RiskLevel.URGENT,
            description = "Recurring fever with chills and sweating. Needs blood test. Take anti-malaria medicine.",
            minDuration = 3, maxDuration = 21,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.10f, AgeGroup.ELDER to 0.05f, AgeGroup.CHILD to 0.05f),
            actionEmergency = "Refer to PHC urgently. Start Chloroquine if available. Monitor consciousness.",
            actionUrgent = "Get RDT/blood smear at PHC within 24h. Start ORS if vomiting.",
            actionNormal = "Monitor fever pattern. If intermittent chills, visit PHC for malaria test.",
        ),
        ConditionNode(
            condition = "Typhoid", conditionLocal = "Typhoid",
            requiredSymptoms = listOf("fever", "belly_pain"),
            optionalSymptoms = listOf("headache", "weakness", "loss_of_appetite", "diarrhea"),
            baseConfidence = 0.65f, riskLevel = RiskLevel.URGENT,
            description = "Continuous fever with stomach pain. Caused by contaminated water. Needs antibiotics.",
            minDuration = 5, maxDuration = 30,
            actionUrgent = "Visit PHC for Widal test. Start Azithromycin if confirmed. Boil drinking water.",
        ),
        ConditionNode(
            condition = "Pneumonia", conditionLocal = "Nimoniya",
            requiredSymptoms = listOf("fever", "cough", "breathlessness"),
            optionalSymptoms = listOf("chest_pain", "weakness"),
            baseConfidence = 0.75f, riskLevel = RiskLevel.EMERGENCY,
            description = "Lung infection with high fever and difficulty breathing. Needs urgent medical care.",
            minDuration = 2, maxDuration = 14,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.10f, AgeGroup.ELDER to 0.08f),
            actionEmergency = "Refer to PHC immediately. Start Amoxicillin if available. Keep patient propped up.",
        ),
        ConditionNode(
            condition = "Tuberculosis (Suspected)", conditionLocal = "TB (Sandighda)",
            requiredSymptoms = listOf("cough", "fever", "weakness"),
            optionalSymptoms = listOf("loss_of_appetite", "chest_pain", "breathlessness", "weight_loss"),
            baseConfidence = 0.55f, riskLevel = RiskLevel.URGENT,
            description = "Persistent cough for more than 2 weeks with fever. Needs sputum test at TB center.",
            minDuration = 14, maxDuration = 365,
            actionUrgent = "Refer to nearest TB center for sputum test. Do not delay. Isolate if possible.",
        ),
        ConditionNode(
            condition = "Diarrheal Disease", conditionLocal = "Dast ki Bimari",
            requiredSymptoms = listOf("diarrhea"),
            optionalSymptoms = listOf("vomiting", "belly_pain", "weakness", "fever"),
            baseConfidence = 0.80f, riskLevel = RiskLevel.URGENT,
            description = "Loose stools causing dehydration. Give ORS immediately. If blood in stool, go to hospital.",
            minDuration = 1, maxDuration = 14,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.10f, AgeGroup.ELDER to 0.05f),
            actionEmergency = "Give ORS immediately. If bloody stool or severe dehydration, refer to PHC.",
            actionUrgent = "Start ORS + Zinc. Monitor hydration. If no improvement in 24h, visit PHC.",
            actionNormal = "Give ORS after each loose stool. Light diet. Follow up in 2 days.",
        ),
        ConditionNode(
            condition = "Anemia", conditionLocal = "Khoon ki Kami",
            requiredSymptoms = listOf("weakness"),
            optionalSymptoms = listOf("breathlessness", "headache", "loss_of_appetite"),
            baseConfidence = 0.50f, riskLevel = RiskLevel.NORMAL,
            description = "Low hemoglobin. Feel tired and weak. Eat iron-rich food and take iron tablets.",
            minDuration = 7, maxDuration = 365,
            sexModifiers = mapOf(SexType.FEMALE to 0.10f),
            ageModifiers = mapOf(AgeGroup.CHILD to 0.05f),
            actionNormal = "Start Iron-Folic Acid tablets. Eat green leafy vegetables, jaggery, dates. Follow up in 2 weeks.",
        ),
        ConditionNode(
            condition = "Hypertension", conditionLocal = "Ucch Raktachap",
            requiredSymptoms = listOf("headache"),
            optionalSymptoms = listOf("chest_pain", "breathlessness", "weakness"),
            baseConfidence = 0.45f, riskLevel = RiskLevel.URGENT,
            description = "High blood pressure. Can cause heart attack or stroke. Check BP regularly.",
            minDuration = 1, maxDuration = 365,
            ageModifiers = mapOf(AgeGroup.ELDER to 0.10f, AgeGroup.ADULT to 0.05f),
            actionEmergency = "If BP > 180/120, refer immediately. Do not exert. Keep calm.",
            actionUrgent = "Visit PHC for BP check. Start Amlodipine if confirmed. Reduce salt intake.",
        ),
        ConditionNode(
            condition = "Diabetes Complication", conditionLocal = "Madhumeh Jatilata",
            requiredSymptoms = listOf("weakness", "burning_urination"),
            optionalSymptoms = listOf("itching", "leg_numbness", "loss_of_appetite"),
            baseConfidence = 0.50f, riskLevel = RiskLevel.URGENT,
            description = "Sugar disease causing body problems. Check blood sugar. See doctor urgently.",
            minDuration = 3, maxDuration = 365,
            ageModifiers = mapOf(AgeGroup.ELDER to 0.08f),
            actionUrgent = "Get blood sugar tested at PHC. If known diabetic, check medication compliance.",
        ),
        ConditionNode(
            condition = "Skin Infection (Fungal)", conditionLocal = "Chamdi ka Sankraman",
            requiredSymptoms = listOf("rash", "itching"),
            optionalSymptoms = listOf("swelling"),
            baseConfidence = 0.75f, riskLevel = RiskLevel.NORMAL,
            description = "Fungal infection causing itching and rash. Keep area clean and dry. Apply antifungal cream.",
            actionNormal = "Apply Clotrimazole cream twice daily for 2-4 weeks. Keep skin dry. Wear loose clothes.",
        ),
        ConditionNode(
            condition = "Conjunctivitis", conditionLocal = "Aankh Aana",
            requiredSymptoms = listOf("eye_discharge"),
            optionalSymptoms = listOf("itching", "headache"),
            baseConfidence = 0.82f, riskLevel = RiskLevel.NORMAL,
            description = "Eye infection causing redness and discharge. Wash eyes with clean water. Use eye drops.",
            actionNormal = "Use Gentamicin eye drops every 4 hours. Wash hands frequently. Do not share towels.",
        ),
        ConditionNode(
            condition = "Urinary Tract Infection", conditionLocal = "Mutra Marg Sankraman",
            requiredSymptoms = listOf("burning_urination"),
            optionalSymptoms = listOf("fever", "belly_pain", "back_pain"),
            baseConfidence = 0.72f, riskLevel = RiskLevel.URGENT,
            description = "Infection in urinary system. Pain while urinating. Drink lots of water.",
            sexModifiers = mapOf(SexType.FEMALE to 0.08f),
            actionUrgent = "Start Nitrofurantoin if available. Drink 3+ liters water daily. Visit PHC for urine test.",
        ),
        ConditionNode(
            condition = "Snakebite", conditionLocal = "Saanp ka Katna",
            requiredSymptoms = listOf("swelling"),
            optionalSymptoms = listOf("bleeding", "weakness", "vomiting"),
            baseConfidence = 0.40f, riskLevel = RiskLevel.EMERGENCY,
            description = "If bitten by snake, go to hospital IMMEDIATELY. Do not tie the bite area. Keep calm.",
            minDuration = 0, maxDuration = 1,
            actionEmergency = "Rush to nearest hospital with anti-venom. Do NOT tie tourniquet. Keep limb immobilized below heart level.",
        ),
        ConditionNode(
            condition = "Heat Stroke", conditionLocal = "Lu Lagna",
            requiredSymptoms = listOf("fever", "headache", "weakness"),
            optionalSymptoms = listOf("vomiting", "unconsciousness"),
            baseConfidence = 0.60f, riskLevel = RiskLevel.EMERGENCY,
            description = "Body overheated from sun. Move to shade. Pour cool water on body. Give ORS.",
            minDuration = 0, maxDuration = 3,
            ageModifiers = mapOf(AgeGroup.ELDER to 0.10f, AgeGroup.INFANT to 0.10f),
            actionEmergency = "Move to shade immediately. Pour cool (not cold) water on body. Give ORS. Fan the patient. Refer to PHC.",
        ),
        ConditionNode(
            condition = "Malnutrition", conditionLocal = "Kuposhan",
            requiredSymptoms = listOf("weakness", "loss_of_appetite"),
            optionalSymptoms = listOf("swelling", "rash"),
            baseConfidence = 0.55f, riskLevel = RiskLevel.URGENT,
            description = "Not getting enough nutrition. Body is weak. Need proper food and supplements.",
            minDuration = 14, maxDuration = 365,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.15f, AgeGroup.CHILD to 0.10f),
            actionUrgent = "Start supplementary feeding. Give Iron-Folic Acid and Vitamin A. Refer to NRC if severe.",
        ),
        ConditionNode(
            condition = "Gastroenteritis", conditionLocal = "Pet ka Sankraman",
            requiredSymptoms = listOf("vomiting", "belly_pain"),
            optionalSymptoms = listOf("diarrhea", "fever", "weakness"),
            baseConfidence = 0.73f, riskLevel = RiskLevel.URGENT,
            description = "Stomach flu causing vomiting and pain. Give ORS. Avoid oily food.",
            actionUrgent = "Start ORS. If persistent vomiting, give Ondansetron. Bland diet only. Visit PHC if no improvement.",
        ),
        ConditionNode(
            condition = "Ear Infection", conditionLocal = "Kaan ka Sankraman",
            requiredSymptoms = listOf("ear_pain"),
            optionalSymptoms = listOf("fever", "headache"),
            baseConfidence = 0.78f, riskLevel = RiskLevel.NORMAL,
            description = "Infection inside ear causing pain. Do not put anything inside. See doctor for ear drops.",
            ageModifiers = mapOf(AgeGroup.CHILD to 0.05f),
            actionNormal = "Give Paracetamol for pain. Do not insert anything in ear. Visit PHC for ear drops if no improvement.",
        ),
        ConditionNode(
            condition = "Dental Infection", conditionLocal = "Daant ka Sankraman",
            requiredSymptoms = listOf("mouth_sore"),
            optionalSymptoms = listOf("fever", "swelling", "headache"),
            baseConfidence = 0.76f, riskLevel = RiskLevel.NORMAL,
            description = "Tooth or mouth infection causing pain. Rinse with warm salt water. See dentist.",
            actionNormal = "Rinse with warm salt water 3-4 times daily. Give Paracetamol for pain. Visit dentist.",
        ),
        ConditionNode(
            condition = "Seizure Disorder", conditionLocal = "Daure ki Bimari",
            requiredSymptoms = listOf("seizures"),
            optionalSymptoms = listOf("unconsciousness", "headache"),
            baseConfidence = 0.80f, riskLevel = RiskLevel.EMERGENCY,
            description = "Fits/seizures need immediate medical help. Turn person on side. Do not put anything in mouth.",
            ageModifiers = mapOf(AgeGroup.INFANT to 0.05f, AgeGroup.CHILD to 0.05f),
            actionEmergency = "Turn on side. Clear area of sharp objects. Do NOT put anything in mouth. Time the seizure. Refer to PHC.",
        ),
        ConditionNode(
            condition = "Common Cold", conditionLocal = "Thanda Jwar",
            requiredSymptoms = listOf("cough"),
            optionalSymptoms = listOf("fever", "headache", "throat_pain"),
            baseConfidence = 0.60f, riskLevel = RiskLevel.NORMAL,
            description = "Viral cold with cough. Rest and drink warm fluids. Usually gets better in 5-7 days.",
            minDuration = 1, maxDuration = 10,
            actionNormal = "Rest. Warm fluids. Paracetamol if fever. Follow up if not better in 5-7 days.",
        ),
        ConditionNode(
            condition = "Cholera", conditionLocal = "Haija",
            requiredSymptoms = listOf("diarrhea", "vomiting"),
            optionalSymptoms = listOf("weakness", "belly_pain"),
            baseConfidence = 0.65f, riskLevel = RiskLevel.EMERGENCY,
            description = "Severe watery diarrhea causing rapid dehydration. Life-threatening if untreated.",
            minDuration = 0, maxDuration = 7,
            ageModifiers = mapOf(AgeGroup.INFANT to 0.10f, AgeGroup.ELDER to 0.08f),
            actionEmergency = "Start ORS immediately and continuously. Refer to hospital. IV fluids may be needed.",
        ),
    )
}
