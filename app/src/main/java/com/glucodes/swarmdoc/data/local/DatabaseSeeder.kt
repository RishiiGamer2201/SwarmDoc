package com.glucodes.swarmdoc.data.local

import com.glucodes.swarmdoc.data.local.dao.*
import com.glucodes.swarmdoc.data.local.entities.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val patientDao: PatientDao,
    private val consultationDao: ConsultationDao,
    private val symptomEntryDao: SymptomEntryDao,
    private val meshSyncDao: MeshSyncDao,
    private val alertDao: AlertDao,
    private val medicineDao: MedicineDao,
    private val conditionMappingDao: ConditionMedicineMappingDao,
    private val doctorRegistryDao: DoctorRegistryDao,
) {
    private val json = Json { prettyPrint = false }

    suspend fun seedIfNeeded() {
        if (patientDao.getPatientCount() == 0) {
            seedPatients()
            seedMeshPackets()
            seedAlerts()
        }
        if (medicineDao.getMedicineCount() == 0) {
            seedMedicines()
        }
        if (conditionMappingDao.getMappingCount() == 0) {
            seedConditionMedicineMappings()
        }
        if (doctorRegistryDao.getDoctorCount() == 0) {
            seedDoctors()
        }
    }

    private suspend fun seedPatients() {
        val now = System.currentTimeMillis()
        val day = 86400000L

        val p1 = PatientEntity(name = "Ramesh Kumar", initials = "RK", age = 35, sex = "MALE", village = "Brahmagiri", district = "Puri", createdAt = now - 6 * day)
        val p1Id = patientDao.insertPatient(p1)
        val c1 = ConsultationEntity(
            patientId = p1Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("fever", "joint_pain", "rash")),
            durationDays = 4, riskLevel = "EMERGENCY", ageAtVisit = 35, sex = "MALE",
            topDiagnosis = "Dengue Fever", diagnosisConfidence = 0.88f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Dengue Fever","nameLocal":"Dengue Jwar","confidence":0.88,"description":"High fever with joint pain and rash, common in monsoon season"}],"riskLevel":"EMERGENCY"}""",
            communityContextJson = """{"similarCasesCount":4,"clusterDetected":true,"clusterMessage":"4 cases with similar symptoms detected in your area this week - possible dengue cluster","affectedWorkers":2}""",
            recommendedAction = "Refer to PHC immediately. Start ORS. Monitor platelet count.",
            timestamp = now - 6 * day
        )
        val c1Id = consultationDao.insertConsultation(c1)
        symptomEntryDao.insertSymptomEntries(listOf(
            SymptomEntryEntity(consultationId = c1Id, symptomName = "fever", duration = 4),
            SymptomEntryEntity(consultationId = c1Id, symptomName = "joint_pain", duration = 4),
            SymptomEntryEntity(consultationId = c1Id, symptomName = "rash", duration = 2),
        ))

        val p2 = PatientEntity(name = "Sunita Devi", initials = "SD", age = 28, sex = "FEMALE", village = "Brahmagiri", district = "Puri", createdAt = now - 5 * day)
        val p2Id = patientDao.insertPatient(p2)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p2Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("fever", "joint_pain", "rash", "headache")),
            durationDays = 3, riskLevel = "EMERGENCY", ageAtVisit = 28, sex = "FEMALE",
            topDiagnosis = "Dengue Fever", diagnosisConfidence = 0.91f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Dengue Fever","nameLocal":"Dengue Jwar","confidence":0.91,"description":"Severe fever with body pain and rash"}],"riskLevel":"EMERGENCY"}""",
            communityContextJson = """{"similarCasesCount":3,"clusterDetected":true,"clusterMessage":"3 cases with similar symptoms detected - possible dengue cluster","affectedWorkers":2}""",
            timestamp = now - 5 * day
        ))

        val p3 = PatientEntity(name = "Gopal Singh", initials = "GS", age = 42, sex = "MALE", village = "Nimapara", district = "Puri", createdAt = now - 4 * day)
        val p3Id = patientDao.insertPatient(p3)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p3Id, workerId = "asha_002",
            symptomsJson = json.encodeToString(listOf("fever", "joint_pain", "rash", "vomiting")),
            durationDays = 5, riskLevel = "EMERGENCY", ageAtVisit = 42, sex = "MALE",
            topDiagnosis = "Dengue Fever", diagnosisConfidence = 0.85f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Dengue Fever","nameLocal":"Dengue Jwar","confidence":0.85,"description":"Fever with joint pain and skin rash"}],"riskLevel":"EMERGENCY"}""",
            timestamp = now - 4 * day
        ))

        val p4 = PatientEntity(name = "Lakshmi Bai", initials = "LB", age = 55, sex = "FEMALE", village = "Pipili", district = "Puri", createdAt = now - 3 * day)
        val p4Id = patientDao.insertPatient(p4)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p4Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("fever", "weakness", "vomiting", "headache")),
            durationDays = 7, riskLevel = "URGENT", ageAtVisit = 55, sex = "FEMALE",
            topDiagnosis = "Malaria", diagnosisConfidence = 0.72f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Malaria","nameLocal":"Malaria","confidence":0.72,"description":"Recurring fever with chills and weakness"}],"riskLevel":"URGENT"}""",
            timestamp = now - 3 * day
        ))

        val p5 = PatientEntity(name = "Arjun Meena", initials = "AM", age = 12, sex = "MALE", village = "Brahmagiri", district = "Puri", createdAt = now - 1 * day)
        val p5Id = patientDao.insertPatient(p5)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p5Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("cough", "fever", "headache")),
            durationDays = 2, riskLevel = "NORMAL", ageAtVisit = 12, sex = "MALE",
            topDiagnosis = "Common Cold", diagnosisConfidence = 0.80f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Common Cold","nameLocal":"Thanda Jwar","confidence":0.80,"description":"Mild fever with cough, likely viral infection"}],"riskLevel":"NORMAL"}""",
            timestamp = now - 1 * day, followUpDate = now + 3 * day,
        ))

        val p6 = PatientEntity(name = "Priya Sharma", initials = "PS", age = 30, sex = "FEMALE", village = "Nimapara", district = "Puri", createdAt = now - 2 * day)
        val p6Id = patientDao.insertPatient(p6)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p6Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("belly_pain", "vomiting", "diarrhea")),
            durationDays = 2, riskLevel = "URGENT", ageAtVisit = 30, sex = "FEMALE",
            topDiagnosis = "Gastroenteritis", diagnosisConfidence = 0.75f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Gastroenteritis","nameLocal":"Pet ka Sankraman","confidence":0.75,"description":"Stomach infection with vomiting and loose stools"}],"riskLevel":"URGENT"}""",
            timestamp = now - 2 * day
        ))

        val p7 = PatientEntity(name = "Bhola Ram", initials = "BR", age = 65, sex = "MALE", village = "Pipili", district = "Puri", createdAt = now - 1 * day)
        val p7Id = patientDao.insertPatient(p7)
        consultationDao.insertConsultation(ConsultationEntity(
            patientId = p7Id, workerId = "asha_001",
            symptomsJson = json.encodeToString(listOf("chest_pain", "breathlessness", "weakness")),
            durationDays = 1, riskLevel = "EMERGENCY", ageAtVisit = 65, sex = "MALE",
            topDiagnosis = "Hypertensive Crisis", diagnosisConfidence = 0.68f,
            diagnosisJson = """{"conditions":[{"nameEnglish":"Hypertensive Crisis","nameLocal":"Ucch Raktachap","confidence":0.68,"description":"Chest pain with difficulty breathing, may indicate heart problem"}],"riskLevel":"EMERGENCY"}""",
            timestamp = now - 1 * day
        ))
    }

    private suspend fun seedMeshPackets() {
        val now = System.currentTimeMillis()
        val day = 86400000L
        val packets = listOf(
            MeshSyncPacketEntity(sourceWorkerIdHash = "w_hash_002", anonymizedSymptomsJson = """["fever","joint_pain","rash"]""", conditionSuspected = "Dengue", caseCount = 2, gpsZone = "PURI_ZONE_A", timestamp = now - 5 * day),
            MeshSyncPacketEntity(sourceWorkerIdHash = "w_hash_003", anonymizedSymptomsJson = """["fever","joint_pain","rash","headache"]""", conditionSuspected = "Dengue", caseCount = 1, gpsZone = "PURI_ZONE_A", timestamp = now - 4 * day),
            MeshSyncPacketEntity(sourceWorkerIdHash = "w_hash_002", anonymizedSymptomsJson = """["fever","cough","breathlessness"]""", conditionSuspected = "Pneumonia", caseCount = 1, gpsZone = "PURI_ZONE_B", timestamp = now - 3 * day),
            MeshSyncPacketEntity(sourceWorkerIdHash = "w_hash_004", anonymizedSymptomsJson = """["fever","joint_pain","rash"]""", conditionSuspected = "Dengue", caseCount = 1, gpsZone = "PURI_ZONE_A", timestamp = now - 2 * day),
            MeshSyncPacketEntity(sourceWorkerIdHash = "w_hash_001", anonymizedSymptomsJson = """["fever","joint_pain","rash"]""", conditionSuspected = "Dengue", caseCount = 3, gpsZone = "PURI_ZONE_A", timestamp = now - 1 * day),
        )
        meshSyncDao.insertPackets(packets)
    }

    private suspend fun seedAlerts() {
        alertDao.insertAlerts(listOf(
            AlertRecordEntity(
                condition = "Dengue Fever",
                caseCount = 6,
                region = "Puri Zone A (Brahmagiri, Nimapara)",
                isActive = true,
                message = "Dengue cluster detected: 6 cases in Brahmagiri-Nimapara area. Possible outbreak - escalate to PHC immediately.",
                timestamp = System.currentTimeMillis()
            )
        ))
    }

    private suspend fun seedDoctors() {
        doctorRegistryDao.insertDoctors(listOf(
            DoctorRegistryEntity(name = "Dr. Anita Sahu", phone = "+919876543210", phcName = "Puri PHC", phcLat = 19.8135, phcLng = 85.8312, currentStatus = "FREE"),
            DoctorRegistryEntity(name = "Dr. Ramesh Patnaik", phone = "+919876543211", phcName = "Puri PHC", phcLat = 19.8135, phcLng = 85.8312, currentStatus = "BUSY_LOW_RISK", currentPatientId = 5),
            DoctorRegistryEntity(name = "Dr. Sunita Pradhan", phone = "+919876543212", phcName = "Nimapara CHC", phcLat = 20.0564, phcLng = 86.0134, currentStatus = "FREE"),
            DoctorRegistryEntity(name = "Dr. Ajay Mehra", phone = "+919876543213", phcName = "Nimapara CHC", phcLat = 20.0564, phcLng = 86.0134, currentStatus = "BUSY_HIGH_RISK"),
            DoctorRegistryEntity(name = "Dr. Kavita Reddy", phone = "+919876543214", phcName = "Pipili PHC", phcLat = 20.1173, phcLng = 85.8317, currentStatus = "FREE"),
            DoctorRegistryEntity(name = "Dr. Sanjay Nanda", phone = "+919876543215", phcName = "Pipili PHC", phcLat = 20.1173, phcLng = 85.8317, currentStatus = "FREE"),
        ))
    }

    private suspend fun seedMedicines() {
        val meds = listOf(
            m("Paracetamol", "Paracetamol", "Antipyretic", "500mg every 6 hours", "10mg/kg every 6 hours", "500mg every 8 hours", "Liver disease", true, true, false),
            m("Ibuprofen", "Ibuprofen", "Antipyretic", "400mg every 8 hours", "5-10mg/kg every 8 hours", "200mg every 8 hours", "Peptic ulcer, renal disease", true, true, false),
            m("Amoxicillin", "Amoxicillin", "Antibiotic", "500mg every 8 hours for 5 days", "25mg/kg/day divided 8 hourly", "500mg every 8 hours", "Penicillin allergy", true, true, true),
            m("Azithromycin", "Azithromycin", "Antibiotic", "500mg day 1, then 250mg for 4 days", "10mg/kg day 1, then 5mg/kg", "500mg day 1, then 250mg", "Liver disease", true, false, true),
            m("Metronidazole", "Metronidazole", "Antibiotic", "400mg every 8 hours for 7 days", "7.5mg/kg every 8 hours", "400mg every 8 hours", "First trimester pregnancy", true, true, true),
            m("Ciprofloxacin", "Ciprofloxacin", "Antibiotic", "500mg every 12 hours for 5 days", "Not recommended under 18", "500mg every 12 hours", "Pregnancy, children under 18", true, false, true),
            m("Doxycycline", "Doxycycline", "Antibiotic", "100mg every 12 hours for 7 days", "Not for children under 8", "100mg every 12 hours", "Pregnancy, children under 8", true, false, true),
            m("Cotrimoxazole", "Cotrimoxazole", "Antibiotic", "960mg every 12 hours", "24mg/kg every 12 hours", "960mg every 12 hours", "Sulfa allergy, renal failure", true, true, true),
            m("Cefixime", "Cefixime", "Antibiotic", "200mg every 12 hours", "8mg/kg/day", "200mg every 12 hours", "Cephalosporin allergy", true, false, true),
            m("Gentamicin Eye Drops", "Gentamicin Aankh Drop", "Antibiotic", "1-2 drops every 4 hours", "1 drop every 4 hours", "1-2 drops every 4 hours", "Hypersensitivity", true, true, false),
            m("Chloramphenicol Eye Drops", "Chloramphenicol Aankh Drop", "Antibiotic", "1-2 drops every 6 hours", "1 drop every 6 hours", "1-2 drops every 6 hours", "Aplastic anemia history", true, true, false),
            m("ORS", "ORS (Jeevan Jal)", "ORS", "After each loose stool, 200-400ml", "After each loose stool, 50-100ml", "After each loose stool, 200ml", "None", true, true, false),
            m("Zinc Tablets", "Zinc Goli", "Vitamin", "20mg daily for 14 days", "10mg under 6m, 20mg over 6m daily for 14 days", "20mg daily for 14 days", "None significant", true, true, false),
            m("Iron-Folic Acid", "Iron-Folic Acid", "Vitamin", "1 tablet daily", "Age-appropriate dose daily", "1 tablet daily", "Hemochromatosis", true, true, false),
            m("Albendazole", "Albendazole", "Antiparasitic", "400mg single dose", "200mg if 1-2 years, 400mg if over 2 years", "400mg single dose", "First trimester pregnancy", true, true, false),
            m("Mebendazole", "Mebendazole", "Antiparasitic", "100mg twice daily for 3 days", "100mg twice daily for 3 days if over 2", "100mg twice daily for 3 days", "Pregnancy", true, true, false),
            m("Omeprazole", "Omeprazole", "Antacid", "20mg once daily before food", "1mg/kg/day", "20mg once daily", "Long-term use caution", true, false, true),
            m("Ranitidine", "Ranitidine", "Antacid", "150mg twice daily", "2mg/kg twice daily", "150mg twice daily", "Liver disease", true, true, false),
            m("Antacid Gel", "Antacid Gel", "Antacid", "10ml after meals", "5ml after meals", "10ml after meals", "Renal failure", true, true, false),
            m("Amlodipine", "Amlodipine", "Antihypertensive", "5mg once daily", "Not standard for children", "2.5mg once daily", "Severe aortic stenosis", true, false, true),
            m("Atenolol", "Atenolol", "Antihypertensive", "50mg once daily", "Not standard for children", "25mg once daily", "Asthma, heart block", true, false, true),
            m("Enalapril", "Enalapril", "Antihypertensive", "5mg once daily", "0.08mg/kg/day", "2.5mg once daily", "Pregnancy, bilateral renal artery stenosis", true, false, true),
            m("Losartan", "Losartan", "Antihypertensive", "50mg once daily", "Not under 6 years", "25mg once daily", "Pregnancy", true, false, true),
            m("Hydrochlorothiazide", "Hydrochlorothiazide", "Antihypertensive", "12.5-25mg once daily", "Not standard for children", "12.5mg once daily", "Gout, renal failure", true, false, true),
            m("Metformin", "Metformin", "Antidiabetic", "500mg twice daily with food", "Not standard for children", "500mg once daily", "Renal failure, liver disease", true, false, true),
            m("Glibenclamide", "Glibenclamide", "Antidiabetic", "2.5mg once daily", "Not for children", "1.25mg once daily", "Type 1 diabetes, renal failure", true, false, true),
            m("Glimepiride", "Glimepiride", "Antidiabetic", "1mg once daily", "Not for children", "1mg once daily", "Type 1 diabetes", true, false, true),
            m("Atorvastatin", "Atorvastatin", "Lipid Lowering", "10mg once daily at night", "Not for children", "10mg once daily", "Active liver disease, pregnancy", true, false, true),
            m("Cetirizine", "Cetirizine", "Antihistamine", "10mg once daily", "5mg once daily if 6-12", "10mg once daily", "Severe renal impairment", true, true, false),
            m("Chlorpheniramine", "Chlorpheniramine", "Antihistamine", "4mg every 6 hours", "2mg every 6 hours", "4mg every 8 hours", "Narrow-angle glaucoma", true, true, false),
            m("Salbutamol Inhaler", "Salbutamol Inhaler", "Bronchodilator", "2 puffs every 4-6 hours as needed", "1-2 puffs every 4-6 hours", "2 puffs every 4-6 hours", "Hyperthyroidism", true, false, false),
            m("Salbutamol Tablet", "Salbutamol Goli", "Bronchodilator", "2mg every 8 hours", "1mg every 8 hours if 2-6y", "2mg every 8 hours", "Hyperthyroidism", true, true, false),
            m("Aminophylline", "Aminophylline", "Bronchodilator", "100mg every 8 hours", "Not standard for children", "100mg every 12 hours", "Peptic ulcer, seizure disorder", true, false, true),
            m("Betamethasone Cream", "Betamethasone Cream", "Topical", "Apply thin layer twice daily", "Apply thin layer once daily, short duration", "Apply thin layer once daily", "Skin infections, long-term use", true, true, false),
            m("Clotrimazole Cream", "Clotrimazole Cream", "Antifungal", "Apply twice daily for 2-4 weeks", "Apply twice daily for 2-4 weeks", "Apply twice daily for 2-4 weeks", "None significant", true, true, false),
            m("Miconazole Cream", "Miconazole Cream", "Antifungal", "Apply twice daily for 2 weeks", "Apply twice daily for 2 weeks", "Apply twice daily for 2 weeks", "None significant", true, true, false),
            m("Fluconazole", "Fluconazole", "Antifungal", "150mg single dose", "3-6mg/kg/day", "150mg single dose", "Liver disease, pregnancy", true, false, true),
            m("Permethrin Cream", "Permethrin Cream", "Antiparasitic", "Apply all over, wash after 8-12 hours", "Apply all over, wash after 8-12 hours", "Apply all over, wash after 8-12 hours", "None significant", true, true, false),
            m("Povidone Iodine", "Povidone Iodine", "Topical", "Apply to wound as needed", "Apply to wound as needed", "Apply to wound as needed", "Thyroid disorder", true, true, false),
            m("Silver Sulfadiazine", "Silver Sulfadiazine", "Topical", "Apply to burn once daily", "Apply to burn once daily", "Apply to burn once daily", "Sulfa allergy, pregnancy near term", true, false, false),
            m("Calamine Lotion", "Calamine Lotion", "Topical", "Apply 3-4 times daily", "Apply 3-4 times daily", "Apply 3-4 times daily", "None", true, true, false),
            m("Magnesium Sulfate", "Magnesium Sulfate", "Emergency", "4g IV loading dose", "Not for children", "4g IV loading dose", "Heart block, renal failure", true, false, true),
            m("Oxytocin", "Oxytocin", "Emergency", "10 IU IM after delivery", "Not applicable", "10 IU IM after delivery", "Before delivery of baby", true, false, true),
            m("Misoprostol", "Misoprostol", "Emergency", "600mcg sublingual if no oxytocin", "Not applicable", "600mcg sublingual", "Before delivery", true, false, true),
            m("Diazepam", "Diazepam", "Emergency", "5-10mg IV for seizures", "0.3mg/kg rectal/IV", "5mg IV", "Respiratory depression, sleep apnea", true, false, true),
            m("Adrenaline", "Adrenaline", "Emergency", "0.5mg IM for anaphylaxis", "0.01mg/kg IM", "0.3mg IM", "None in emergency", true, false, true),
            m("Vitamin A", "Vitamin A", "Vitamin", "200,000 IU every 6 months", "100,000 IU if 6-12m, 200,000 IU if over 1y", "Not routine", "Pregnancy", true, true, false),
            m("Vitamin D", "Vitamin D", "Vitamin", "60,000 IU weekly for 8 weeks", "400-1000 IU daily", "60,000 IU weekly for 8 weeks", "Hypercalcemia", true, false, false),
            m("Calcium Tablets", "Calcium Goli", "Vitamin", "500mg twice daily", "250-500mg daily", "500mg twice daily", "Hypercalcemia, kidney stones", true, true, false),
            m("Folic Acid", "Folic Acid", "Vitamin", "5mg daily", "Age-appropriate dose", "5mg daily", "None significant", true, true, false),
            m("Vitamin B Complex", "Vitamin B Complex", "Vitamin", "1 tablet daily", "1 tablet daily", "1 tablet daily", "None significant", true, true, false),
            m("Vitamin C", "Vitamin C", "Vitamin", "500mg daily", "Age-appropriate dose", "500mg daily", "Kidney stones history", true, true, false),
            m("Domperidone", "Domperidone", "Antiemetic", "10mg before meals 3 times daily", "0.25mg/kg 3 times daily", "10mg before meals", "GI hemorrhage, cardiac arrhythmia", true, false, true),
            m("Ondansetron", "Ondansetron", "Antiemetic", "4mg every 8 hours", "0.1mg/kg every 8 hours", "4mg every 8 hours", "Congenital long QT", true, false, true),
            m("Loperamide", "Loperamide", "Antidiarrheal", "4mg initial, then 2mg after each stool", "Not under 2 years", "2mg after each stool", "Bloody diarrhea, fever", true, false, false),
            m("Chloroquine", "Chloroquine", "Antimalarial", "600mg day 1, 300mg day 2 and 3", "10mg/kg day 1, 5mg/kg day 2 and 3", "300mg day 1, 150mg day 2 and 3", "Retinopathy, psoriasis", true, false, true),
            m("Artemether-Lumefantrine", "Artemether-Lumefantrine", "Antimalarial", "4 tablets twice daily for 3 days", "Weight-based dosing", "4 tablets twice daily for 3 days", "First trimester pregnancy", true, false, true),
            m("Primaquine", "Primaquine", "Antimalarial", "15mg daily for 14 days", "0.25mg/kg daily for 14 days", "15mg daily for 14 days", "G6PD deficiency, pregnancy", true, false, true),
            m("Quinine", "Quinine", "Antimalarial", "600mg every 8 hours for 7 days", "10mg/kg every 8 hours", "600mg every 8 hours", "Myasthenia gravis, optic neuritis", true, false, true),
            m("Isoniazid", "Isoniazid (INH)", "Anti-TB", "5mg/kg daily (max 300mg)", "10mg/kg daily", "5mg/kg daily", "Active hepatitis", true, false, true),
            m("Rifampicin", "Rifampicin", "Anti-TB", "10mg/kg daily (max 600mg)", "15mg/kg daily", "10mg/kg daily", "Jaundice", true, false, true),
            m("Pyrazinamide", "Pyrazinamide", "Anti-TB", "25mg/kg daily", "35mg/kg daily", "25mg/kg daily", "Severe liver damage, gout", true, false, true),
            m("Ethambutol", "Ethambutol", "Anti-TB", "15mg/kg daily", "20mg/kg daily", "15mg/kg daily", "Optic neuritis, under 6 years", true, false, true),
            m("Streptomycin", "Streptomycin", "Anti-TB", "15mg/kg IM daily", "20mg/kg IM daily", "10mg/kg IM daily", "Pregnancy, renal failure", true, false, true),
            m("Oral Rehydration Salts-Zinc Kit", "ORS-Zinc Kit", "ORS", "ORS after each stool + Zinc 20mg daily", "ORS + Zinc 10mg if under 6m", "ORS after each stool + Zinc 20mg", "None", true, true, false),
            m("Metoclopramide", "Metoclopramide", "Antiemetic", "10mg before meals", "0.1mg/kg", "5mg before meals", "GI obstruction, epilepsy", true, false, true),
            m("Nitrofurantoin", "Nitrofurantoin", "Antibiotic", "100mg every 6 hours for 5 days", "5mg/kg/day divided every 6 hours", "50mg every 6 hours", "Renal failure, G6PD deficiency", true, false, true),
            m("Norfloxacin", "Norfloxacin", "Antibiotic", "400mg every 12 hours for 3 days", "Not for children", "400mg every 12 hours", "Pregnancy, children", true, false, true),
            m("Tramadol", "Tramadol", "Analgesic", "50mg every 6 hours as needed", "Not under 12 years", "25mg every 6 hours", "Seizure disorder, respiratory depression", true, false, true),
            m("Diclofenac", "Diclofenac", "Analgesic", "50mg every 8 hours", "1mg/kg every 8 hours", "25mg every 8 hours", "Peptic ulcer, renal disease, asthma", true, true, false),
            m("Aspirin", "Aspirin", "Analgesic", "300-600mg every 4-6 hours", "Not under 12 years (Reye syndrome)", "75-150mg daily as antiplatelet", "Peptic ulcer, asthma, under 12", true, false, false),
            m("Prednisolone", "Prednisolone", "Steroid", "5-60mg daily depending on condition", "1-2mg/kg/day", "5-10mg daily", "Active infections, peptic ulcer", true, false, true),
            m("Dexamethasone", "Dexamethasone", "Steroid", "4-8mg daily as needed", "0.15mg/kg", "4mg daily", "Active infections", true, false, true),
            m("Hydrocortisone", "Hydrocortisone", "Steroid", "100mg IV in emergency", "50mg IV in emergency", "100mg IV", "Active infections", true, false, true),
            m("Erythromycin", "Erythromycin", "Antibiotic", "500mg every 6 hours", "12.5mg/kg every 6 hours", "500mg every 6 hours", "Liver disease", true, false, true),
            m("Acyclovir", "Acyclovir", "Antiviral", "200mg 5 times daily for 5 days", "20mg/kg 4 times daily", "200mg 5 times daily", "Renal impairment", true, false, true),
            m("Tinidazole", "Tinidazole", "Antibiotic", "2g single dose", "50mg/kg single dose", "2g single dose", "First trimester pregnancy", true, false, true),
            m("Cough Syrup (Dextromethorphan)", "Khansi ki Dawai", "Cough Suppressant", "10ml every 6 hours", "5ml every 6 hours if over 6y", "10ml every 8 hours", "Productive cough", true, true, false),
            m("Benzyl Benzoate Lotion", "Benzyl Benzoate", "Antiparasitic", "Apply all over below neck, wash after 24h", "Dilute 1:1 with water for children", "Apply below neck, wash after 24h", "Excoriated skin", true, true, false),
            m("Ferrous Sulfate", "Ferrous Sulfate", "Vitamin", "200mg twice daily", "3mg/kg/day elemental iron", "200mg once daily", "Hemochromatosis, peptic ulcer", true, true, false),
            m("Mupirocin Ointment", "Mupirocin Ointment", "Topical", "Apply 3 times daily for 5-7 days", "Apply 3 times daily for 5-7 days", "Apply 3 times daily for 5-7 days", "None significant", true, true, false),
        )
        medicineDao.insertMedicines(meds)
    }

    private fun m(
        generic: String, local: String, category: String,
        doseAdult: String, doseChild: String, doseElder: String,
        contra: String, phc: Boolean, sub: Boolean, rx: Boolean
    ) = MedicineEntity(
        genericName = generic, localName = local, category = category,
        standardDoseAdult = doseAdult, standardDoseChild = doseChild,
        standardDoseElder = doseElder, contraindications = contra,
        availableAtPhc = phc, availableAtSubcentre = sub, prescriptionRequired = rx
    )

    private suspend fun seedConditionMedicineMappings() {
        val mappings = listOf(
            cm("Dengue Fever", "Paracetamol", 1, "500mg every 6h for fever", "10mg/kg every 6h", "500mg every 8h"),
            cm("Dengue Fever", "ORS", 2, "200-400ml after each episode", "50-100ml frequently", "200ml frequently"),
            cm("Malaria", "Chloroquine", 1, "600mg day 1, 300mg day 2-3", "10mg/kg day 1, 5mg/kg day 2-3", "300mg day 1, 150mg day 2-3"),
            cm("Malaria", "Paracetamol", 2, "500mg every 6h", "10mg/kg every 6h", "500mg every 8h"),
            cm("Malaria", "Artemether-Lumefantrine", 3, "4 tabs BD for 3 days", "Weight-based", "4 tabs BD for 3 days"),
            cm("Typhoid", "Azithromycin", 1, "500mg day 1, 250mg for 4 days", "10mg/kg day 1, 5mg/kg 4 days", "500mg day 1, 250mg 4 days"),
            cm("Typhoid", "Ciprofloxacin", 2, "500mg BD for 7 days", "Not recommended", "500mg BD"),
            cm("Pneumonia", "Amoxicillin", 1, "500mg every 8h for 5 days", "25mg/kg/day divided 8h", "500mg every 8h"),
            cm("Pneumonia", "Azithromycin", 2, "500mg day 1, 250mg for 4 days", "10mg/kg day 1", "500mg day 1, 250mg"),
            cm("Tuberculosis (Suspected)", "Isoniazid", 1, "5mg/kg daily", "10mg/kg daily", "5mg/kg daily"),
            cm("Tuberculosis (Suspected)", "Rifampicin", 2, "10mg/kg daily", "15mg/kg daily", "10mg/kg daily"),
            cm("Diarrheal Disease", "ORS", 1, "After each loose stool", "After each loose stool", "After each loose stool"),
            cm("Diarrheal Disease", "Zinc Tablets", 2, "20mg daily for 14 days", "10-20mg daily for 14 days", "20mg daily"),
            cm("Anemia", "Iron-Folic Acid", 1, "1 tablet daily", "Age-appropriate", "1 tablet daily"),
            cm("Anemia", "Vitamin C", 2, "500mg with iron tablet", "Age-appropriate", "500mg daily"),
            cm("Hypertension", "Amlodipine", 1, "5mg daily", "Not standard", "2.5mg daily"),
            cm("Hypertension", "Atenolol", 2, "50mg daily", "Not standard", "25mg daily"),
            cm("Skin Infection (Fungal)", "Clotrimazole Cream", 1, "Apply BD for 2-4 weeks", "Apply BD for 2-4 weeks", "Apply BD for 2-4 weeks"),
            cm("Conjunctivitis", "Gentamicin Eye Drops", 1, "1-2 drops every 4h", "1 drop every 4h", "1-2 drops every 4h"),
            cm("Conjunctivitis", "Chloramphenicol Eye Drops", 2, "1-2 drops every 6h", "1 drop every 6h", "1-2 drops every 6h"),
            cm("Urinary Tract Infection", "Nitrofurantoin", 1, "100mg every 6h for 5 days", "5mg/kg/day", "50mg every 6h"),
            cm("Common Cold", "Paracetamol", 1, "500mg every 6h if fever", "10mg/kg every 6h", "500mg every 8h"),
            cm("Common Cold", "Cetirizine", 2, "10mg daily", "5mg daily", "10mg daily"),
            cm("Gastroenteritis", "ORS", 1, "After each episode", "After each episode", "After each episode"),
            cm("Gastroenteritis", "Ondansetron", 2, "4mg every 8h", "0.1mg/kg every 8h", "4mg every 8h"),
            cm("Ear Infection", "Amoxicillin", 1, "500mg every 8h for 5 days", "25mg/kg/day", "500mg every 8h"),
            cm("Dental Infection", "Amoxicillin", 1, "500mg every 8h", "25mg/kg/day", "500mg every 8h"),
            cm("Dental Infection", "Metronidazole", 2, "400mg every 8h", "7.5mg/kg every 8h", "400mg every 8h"),
            cm("Heat Stroke", "ORS", 1, "Rehydrate generously", "Rehydrate", "Rehydrate carefully"),
            cm("Malnutrition", "Iron-Folic Acid", 1, "1 tablet daily", "Age-appropriate", "1 tablet daily"),
            cm("Malnutrition", "Vitamin A", 2, "Not routine", "100,000-200,000 IU", "Not routine"),
            cm("Diabetes Complication", "Metformin", 1, "500mg BD with food", "Not standard", "500mg daily"),
        )
        conditionMappingDao.insertMappings(mappings)
    }

    private fun cm(condition: String, medicine: String, priority: Int, adult: String, child: String, elder: String) =
        ConditionMedicineMappingEntity(
            conditionName = condition, medicineGenericName = medicine,
            priority = priority, notesAdult = adult, notesChild = child, notesElder = elder
        )
}
