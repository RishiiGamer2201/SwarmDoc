package com.glucodes.swarmdoc.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object LanguageSelection : Screen("language_selection")
    object OnboardingWalkthrough : Screen("onboarding_walkthrough")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object PatientHistory : Screen("patient_history")
    object PatientDetail : Screen("patient_detail/{patientId}") {
        fun createRoute(patientId: Long) = "patient_detail/$patientId"
    }
    object PatientMedicalRecord : Screen("patient_medical_record/{patientId}") {
        fun createRoute(patientId: Long) = "patient_medical_record/$patientId"
    }
    object CommunityMesh : Screen("community_mesh")
    object Sos : Screen("sos")
    object WorkerProfile : Screen("worker_profile")
    object MedicineInventory : Screen("medicine_inventory")

    // AI Diagnostics Routes
    object SpecialDiagnostics : Screen("special_diagnostics")
    object CoughScreener : Screen("cough_screener")
    object AnemiaCheck : Screen("anemia_check")
    object RppgVitals : Screen("rppg_vitals")
    object CapillaryRefill : Screen("capillary_refill")

    // Consultation flow (self-contained)
    object ConsultationFlow : Screen("consultation_flow")

    // Legacy routes kept for backward compat during migration
    object DiagnosisResult : Screen("diagnosis_result/{consultationId}") {
        fun createRoute(consultationId: Long) = "diagnosis_result/$consultationId"
    }
    object PatientSummary : Screen("patient_summary/{consultationId}") {
        fun createRoute(consultationId: Long) = "patient_summary/$consultationId"
    }
}
