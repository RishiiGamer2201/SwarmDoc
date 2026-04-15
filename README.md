# SwarmDoc Aarogya Jaal

**Offline First Multimodal AI Health Assistant for ASHA Workers in Rural India**

> Team Glucodes | AIC CureBay Hackathon, APOGEE 2026, BITS Pilani

***

## What is SwarmDoc?

SwarmDoc (subtitle: Aarogya Jaal, HEALTH NET) is an Android application designed for **ASHA (Accredited Social Health Activist) workers** operating in rural India with limited or no internet connectivity. It provides:

* **Offline AI powered medical triage** for 20+ conditions
* **Multimodal symptom input** via voice, text, or photo
* **Community mesh networking** via Bluetooth for outbreak detection
* **SOS emergency alerts** with automatic doctor assignment
* **Medicine inventory tracking** with stock aware recommendations
* **Privacy mode** for sensitive health screenings
* **Advanced Diagnostics** including Camera based Anemia screening, Capillary Refill test, rPPG Heart Rate estimation, and Acoustic Cough Screener for Tuberculosis

The app runs **entirely offline** without internet required for diagnosis, data storage, or mesh sync.

***

## Architecture

  UI Layer (Jetpack Compose, Material 3)       
  * 6 Card Onboarding Walkthrough               
  * 5 Step Consultation Flow                    
  * 4 Tab Bottom Navigation                     
  * Privacy Mode (indigo theme)                 
***
  ViewModel Layer (Hilt injected)               
  * ConsultationViewModel                       
  * HomeViewModel, SosViewModel                 
  * MedicineInventoryViewModel                  
***
  ML Layer (fully offline)                      
  * TriageInferenceEngine                       
  * YamNetClassifier for Audio                  
  * RppgProcessor                               
***
  Data Layer (Room, DataStore)                 
  * PatientEntity, ConsultationEntity           
  * 80+ MedicineEntity                          
  * MeshSyncPacketEntity, AlertRecordEntity     
***
  Mesh Networking Layer                         
  * BLE Advertiser / Scanner                    
  * MeshSyncManager                             
  * Cluster detection                           
***

***

## Tech Stack

* Language: Kotlin 2.0
* UI: Jetpack Compose and Material 3
* DI: Dagger Hilt
* Database: Room v2.6
* Preferences: DataStore
* ML: TensorFlow Lite, ML Kit Face Detection
* Networking: BLE Bluetooth Low Energy
* Camera: CameraX
* Build: Gradle 9.0, KSP 

***

## Key Features

### 1. Onboarding Walkthrough
6 screen swipeable guide explaining the app to first time ASHA workers. Covers consultation flow, risk levels, patient profiles, community radar, and SOS.

### 2. 5 Step Consultation Flow
1. **Patient Basics** Name, age, sex, village, district
2. **Input Mode** Choose Voice, Text, or Photo 
3. **Symptoms** Age branched symptom chips, vitals entry, womens health section with privacy mode
4. **Diagnosis** Real time triage showing risk level, top 3 conditions, community cluster alerts, medicine recommendations
5. **Action** SOS button, PHC referral, WhatsApp share with doctor, save confirmation

### 3. AI Triage Engine
* **20 conditions** including Dengue, Malaria, Typhoid, Pneumonia, Tuberculosis, Anemia, Cholera, Meningitis
* **Age and sex modifiers** adjust confidence scores based on epidemiological risk
* **Under 200ms inference** pure rule based, no model file needed
* **3 tier risk classification**: Emergency, Urgent, Normal

### 4. Community Mesh Networking
* BLE based peer to peer sync between ASHA worker devices
* Anonymized symptom data exchange
* Outbreak detection when 3+ cases of the same condition appear in a zone within 7 days
* Community Health Pulse strip on HomeScreen pulses red during active alerts

### 5. SOS Emergency System
* 4 phase UI: Idle, Confirm, Alerting, Result
* Finds nearest FREE doctor from the registry
* WhatsApp intent to send alert details to the assigned doctor

### 6. Medicine Inventory
* 80+ medicines seeded across categories
* ASHA worker can toggle in stock or out of stock for their centre
* Stock status flows into consultation recommendations

### 7. Female Privacy Mode
* When enabled, UI theme changes from green/gold to indigo/lavender
* Womens health symptoms displayed as coded labels
* Prevents bystander identification of sensitive health screenings

### 8. Advanced Hardware Diagnostics Controls
* **Anemia Eye Test** Built a seamless flip camera toggle control.
* **Capillary Refill Test** Integrated a dedicated interactive Capture button.
* **Persistent Memory Capture** Custom ImageUtils integration hooks with Android MediaStore.

### 9. Dynamic Localization
* Extracted hardcoded values and populated translation maps across 5 supported languages (English, Hindi, Odia, Telugu, Bengali).

***

## Build Instructions

### Prerequisites
* Android Studio Ladybug or newer
* JDK 17
* Android SDK 34

### Debug Build
```bash
./gradlew assembleDemoDebug
```

### Release Build
```bash
./gradlew assembleDemoRelease
```

### YAMNet Model Setup Optional
The cough detection classifier uses Google YAMNet model. To enable it:
1. Download yamnet.tflite from TensorFlow Hub
2. Place it in app/src/main/assets/model/yamnet.tflite
3. Rebuild the app

***

## Design System

* Forest Green (#1B3A2D): Primary, nav, buttons
* Turmeric Gold (#E8A020): Accent, cursor, highlights
* Parchment (#F5EFE0): Surface background
* Coral Red (#D94F3D): Emergency, SOS
* Amber Orange (#E88B20): Urgent risk level
* Sage Green (#6BAF7A): Normal and stable status
* Privacy Indigo (#3D3580): Female privacy mode primary
* Privacy Lavender (#C9C5E8): Female privacy mode accent

***

## Hackathon Context

**Event**: AIC CureBay Hackathon at APOGEE 2026, BITS Pilani

**Problem Statement**: Build an AI powered health assistant that works in areas with no internet connectivity, designed for frontline health workers (ASHA workers) in rural India.

**Our Approach**: SwarmDoc combines offline AI triage with Bluetooth mesh networking to create a swarm intelligence effect. When ASHA workers meet, their devices automatically share anonymized health data, enabling community level outbreak detection that no single worker could achieve alone.

***

## License

This project was built for the AIC CureBay Hackathon at APOGEE 2026, BITS Pilani.
