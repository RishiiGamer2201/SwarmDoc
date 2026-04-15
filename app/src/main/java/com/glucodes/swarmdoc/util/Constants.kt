package com.glucodes.swarmdoc.util

object Constants {
    // Age group enums
    const val AGE_GROUP_INFANT = "INFANT"       // 0-5
    const val AGE_GROUP_CHILD = "CHILD"         // 6-17
    const val AGE_GROUP_ADULT = "ADULT"         // 18-55
    const val AGE_GROUP_ELDER = "ELDER"         // 56+

    fun getAgeGroup(age: Int): String = when {
        age <= 5 -> AGE_GROUP_INFANT
        age <= 17 -> AGE_GROUP_CHILD
        age <= 55 -> AGE_GROUP_ADULT
        else -> AGE_GROUP_ELDER
    }

    // Focus areas
    val FOCUS_AREAS = listOf(
        "General Health",
        "Maternal and Child Health",
        "Elderly Care",
        "Skin and Dermatology",
        "Respiratory and TB",
        "Nutrition and Anemia",
    )

    // Base symptom list (30 symptoms)
    val SYMPTOM_LIST = listOf(
        "fever", "cough", "breathlessness", "joint_pain",
        "rash", "headache", "vomiting", "diarrhea",
        "chest_pain", "weakness", "swelling", "bleeding",
        "loss_of_appetite", "yellow_eyes", "burning_urination", "seizures",
        "unconsciousness", "eye_discharge", "ear_pain", "mouth_sore",
        "belly_pain", "back_pain", "leg_numbness", "itching",
        "feeding_difficulty", "cry_pattern_change", "throat_pain",
        "confusion", "fall_history", "weight_loss",
    )

    // Age-branched symptom sets
    val INFANT_SYMPTOMS = listOf(
        "fever", "feeding_difficulty", "cry_pattern_change", "rash",
        "vomiting", "diarrhea", "breathlessness", "swelling",
        "yellow_eyes", "weakness", "seizures",
    )

    val CHILD_SYMPTOMS = listOf(
        "fever", "cough", "throat_pain", "rash", "belly_pain",
        "headache", "ear_pain", "eye_discharge", "vomiting",
        "diarrhea", "weakness", "breathlessness",
    )

    val ADULT_SYMPTOMS = SYMPTOM_LIST

    val ELDER_SYMPTOMS = listOf(
        "fever", "cough", "breathlessness", "chest_pain",
        "weakness", "headache", "confusion", "fall_history",
        "leg_numbness", "swelling", "back_pain", "belly_pain",
        "burning_urination", "loss_of_appetite", "vomiting",
        "diarrhea", "yellow_eyes", "seizures", "weight_loss",
    )

    // Female-specific symptoms (reproductive)
    val FEMALE_HEALTH_SYMPTOMS = listOf(
        "menstrual_irregularity", "vaginal_discharge",
        "pregnancy_complications", "breast_symptoms",
    )

    // Symptom display names (English)
    val SYMPTOM_DISPLAY_NAMES = mapOf(
        "fever" to "Fever",
        "cough" to "Cough",
        "breathlessness" to "Breathlessness",
        "joint_pain" to "Joint Pain",
        "rash" to "Rash",
        "headache" to "Headache",
        "vomiting" to "Vomiting",
        "diarrhea" to "Diarrhea",
        "chest_pain" to "Chest Pain",
        "weakness" to "Weakness",
        "swelling" to "Swelling",
        "bleeding" to "Bleeding",
        "loss_of_appetite" to "Loss of Appetite",
        "yellow_eyes" to "Yellow Eyes",
        "burning_urination" to "Burning Urination",
        "seizures" to "Fits/Seizures",
        "unconsciousness" to "Unconsciousness",
        "eye_discharge" to "Eye Discharge",
        "ear_pain" to "Ear Pain",
        "mouth_sore" to "Toothache/Mouth Sore",
        "belly_pain" to "Belly Pain",
        "back_pain" to "Back Pain",
        "leg_numbness" to "Leg Numbness",
        "itching" to "Itching",
        "feeding_difficulty" to "Feeding Difficulty",
        "cry_pattern_change" to "Abnormal Cry Pattern",
        "throat_pain" to "Throat Pain",
        "confusion" to "Confusion/Forgetfulness",
        "fall_history" to "Recent Fall",
        "weight_loss" to "Weight Loss",
        "menstrual_irregularity" to "Menstrual Irregularity",
        "vaginal_discharge" to "Vaginal Discharge",
        "pregnancy_complications" to "Pregnancy Complications",
        "breast_symptoms" to "Breast Symptoms",
    )

    // Female privacy coded symptoms
    val PRIVACY_CODED_SYMPTOMS = mapOf(
        "menstrual_irregularity" to "Condition F-1",
        "vaginal_discharge" to "Condition F-2",
        "pregnancy_complications" to "Condition F-3",
        "breast_symptoms" to "Condition F-4",
    )

    // Red flag keywords
    val RED_FLAG_KEYWORDS = listOf(
        "bleeding", "unconsciousness", "seizures", "chest_pain",
        "breathlessness", "high_fever",
    )

    // Risk level thresholds
    const val EMERGENCY_CONFIDENCE_THRESHOLD = 0.7f
    const val URGENT_CONFIDENCE_THRESHOLD = 0.4f

    // Mesh sync
    const val MESH_SYNC_INTERVAL_HOURS = 4
    const val CLUSTER_THRESHOLD = 3

    // Notification channels
    const val CHANNEL_FOLLOWUP = "followup_due"
    const val CHANNEL_ALERT = "community_alert"
    const val CHANNEL_MESH = "mesh_sync"
    const val CHANNEL_SOS = "sos_alert"

    // Demo data
    const val DEMO_WORKER_NAME = "Meena Devi"
    const val DEMO_DISTRICT = "Puri"
    const val DEMO_SUB_DISTRICT = "Puri Sadar"
    const val DEMO_GPS_ZONE = "PURI_ZONE_A"

    // TextField colors for consistent styling (Change 9)
    const val TEXT_COLOR = 0xFF1C1C1C
    const val TEXT_FIELD_CONTAINER = 0xFFF5EFE0
    const val CURSOR_COLOR = 0xFFE8A020
    const val LABEL_COLOR = 0xFF7A7060
    const val FOCUSED_BORDER_COLOR = 0xFFE8A020
    const val UNFOCUSED_BORDER_COLOR = 0xFF7A7060
}
