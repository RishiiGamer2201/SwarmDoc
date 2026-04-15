package com.glucodes.swarmdoc.ml.llm

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Regex-based extraction engine for structuring voice notes.
 * This is the fallback when the LLM (GemmaInterface) is unavailable.
 */
@Singleton
class RegexExtractor @Inject constructor() {

    data class ExtractedData(
        val chiefComplaint: String = "",
        val duration: String = "",
        val vitals: Map<String, String> = emptyMap(),
        val medications: List<String> = emptyList(),
        val redFlags: List<String> = emptyList(),
        val symptoms: List<String> = emptyList(),
        val highlights: List<HighlightSpan> = emptyList(),
    )

    data class HighlightSpan(
        val text: String,
        val category: String, // "symptom", "duration", "vital", "medication", "red_flag"
        val startIndex: Int,
        val endIndex: Int,
    )

    // Symptom keywords (English + Hindi transliterations)
    private val symptomKeywords = mapOf(
        "fever" to listOf("fever", "bukhar", "bukhaar", "temperature", "tapman"),
        "cough" to listOf("cough", "khansi", "khaansi"),
        "headache" to listOf("headache", "sir dard", "sardard", "head pain"),
        "vomiting" to listOf("vomiting", "ulti", "vomit"),
        "diarrhea" to listOf("diarrhea", "diarrhoea", "dast", "loose motion", "loose stool"),
        "breathlessness" to listOf("breathlessness", "saans", "breathing problem", "breathless", "dyspnea"),
        "chest_pain" to listOf("chest pain", "chhati", "seene mein dard"),
        "joint_pain" to listOf("joint pain", "jodo mein dard", "body pain", "body ache"),
        "rash" to listOf("rash", "chaktte", "skin rash", "red spots"),
        "weakness" to listOf("weakness", "kamzori", "weak", "thakan", "fatigue"),
        "swelling" to listOf("swelling", "sujan", "soojhan"),
        "bleeding" to listOf("bleeding", "khoon", "blood"),
        "belly_pain" to listOf("belly pain", "stomach pain", "pet dard", "pet mein dard", "abdominal"),
        "back_pain" to listOf("back pain", "kamar dard", "peeth dard"),
        "burning_urination" to listOf("burning urination", "peshab mein jalan", "urine burn"),
        "seizures" to listOf("seizure", "seizures", "daure", "fits", "convulsion"),
        "unconsciousness" to listOf("unconscious", "behosh", "faint"),
        "itching" to listOf("itching", "khujli", "itch"),
        "yellow_eyes" to listOf("yellow eyes", "peeli aankh", "jaundice", "piliya"),
        "loss_of_appetite" to listOf("no appetite", "bhukh nahi", "loss of appetite", "not eating"),
        "eye_discharge" to listOf("eye discharge", "aankh se paani", "eye water"),
        "ear_pain" to listOf("ear pain", "kaan dard", "ear ache"),
        "leg_numbness" to listOf("leg numbness", "pair sunn", "numbness", "tingling"),
        "mouth_sore" to listOf("mouth sore", "daant dard", "tooth pain", "toothache"),
    )

    // Duration patterns
    private val durationPatterns = listOf(
        Regex("""(\d+)\s*(din|days?|day)""", RegexOption.IGNORE_CASE),
        Regex("""(\d+)\s*(hafta|weeks?|week)""", RegexOption.IGNORE_CASE),
        Regex("""(\d+)\s*(mahina|months?|month)""", RegexOption.IGNORE_CASE),
        Regex("""(ek|do|teen|char|panch|chhe|saat)\s*(din|hafta|mahina)""", RegexOption.IGNORE_CASE),
    )

    // Vital patterns
    private val vitalPatterns = mapOf(
        "bp" to Regex("""(?:bp|blood pressure|raktchap)\s*(?:is|hai|:)?\s*(\d{2,3})\s*/\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
        "temperature" to Regex("""(?:temp|temperature|tapman)\s*(?:is|hai|:)?\s*(\d{2,3}(?:\.\d)?)""", RegexOption.IGNORE_CASE),
        "pulse" to Regex("""(?:pulse|heart rate|dhadkan)\s*(?:is|hai|:)?\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
        "spo2" to Regex("""(?:spo2|oxygen|o2)\s*(?:is|hai|:)?\s*(\d{2,3})""", RegexOption.IGNORE_CASE),
    )

    // Medication patterns
    private val medicationKeywords = listOf(
        "paracetamol", "crocin", "dolo", "combiflam", "ors", "iron tablet",
        "antibiotics", "amoxicillin", "metformin", "amlodipine", "aspirin",
        "cetirizine", "azithromycin", "ibuprofen", "antacid", "omeprazole",
    )

    // Red flag keywords
    private val redFlagKeywords = listOf(
        "bleeding", "khoon", "unconscious", "behosh", "seizure", "daure",
        "fits", "chest pain", "chhati dard", "breathless", "saans nahi",
        "high fever", "bahut bukhar", "not breathing",
    )

    fun extract(text: String): ExtractedData {
        val lowerText = text.lowercase()
        val highlights = mutableListOf<HighlightSpan>()

        // Extract symptoms
        val foundSymptoms = mutableListOf<String>()
        for ((symptomId, keywords) in symptomKeywords) {
            for (keyword in keywords) {
                val idx = lowerText.indexOf(keyword)
                if (idx >= 0) {
                    foundSymptoms.add(symptomId)
                    highlights.add(HighlightSpan(keyword, "symptom", idx, idx + keyword.length))
                    break
                }
            }
        }

        // Extract duration
        var duration = ""
        for (pattern in durationPatterns) {
            val match = pattern.find(lowerText)
            if (match != null) {
                duration = match.value
                highlights.add(HighlightSpan(match.value, "duration", match.range.first, match.range.last + 1))
                break
            }
        }

        // Extract vitals
        val vitals = mutableMapOf<String, String>()
        for ((vitalName, pattern) in vitalPatterns) {
            val match = pattern.find(lowerText)
            if (match != null) {
                vitals[vitalName] = match.groupValues.drop(1).joinToString("/")
                highlights.add(HighlightSpan(match.value, "vital", match.range.first, match.range.last + 1))
            }
        }

        // Extract medications
        val medications = medicationKeywords.filter { lowerText.contains(it) }
        for (med in medications) {
            val idx = lowerText.indexOf(med)
            if (idx >= 0) {
                highlights.add(HighlightSpan(med, "medication", idx, idx + med.length))
            }
        }

        // Extract red flags
        val redFlags = redFlagKeywords.filter { lowerText.contains(it) }
        for (flag in redFlags) {
            val idx = lowerText.indexOf(flag)
            if (idx >= 0) {
                highlights.add(HighlightSpan(flag, "red_flag", idx, idx + flag.length))
            }
        }

        // Determine chief complaint (first / most severe symptom)
        val chiefComplaint = when {
            redFlags.isNotEmpty() -> redFlags.first()
            foundSymptoms.isNotEmpty() -> foundSymptoms.first()
            else -> ""
        }

        return ExtractedData(
            chiefComplaint = chiefComplaint,
            duration = duration,
            vitals = vitals,
            medications = medications,
            redFlags = redFlags,
            symptoms = foundSymptoms.distinct(),
            highlights = highlights.sortedBy { it.startIndex },
        )
    }
}
