# YAMNet Audio Classification Model

## Model: YAMNet (Yet Another Mobile Network)

YAMNet is Google's pre-trained audio event classifier that recognizes 521 audio event classes.
We use it to detect and classify cough sounds.

## Download

1. Visit: https://tfhub.dev/google/lite-model/yamnet/tflite/1
2. Download the `yamnet.tflite` file (~13MB)
3. Place it at: `app/src/main/assets/model/yamnet.tflite`

## Alternative download (direct):
```
wget https://storage.googleapis.com/tfhub-lite-models/google/lite-model/yamnet/tflite/1.tflite -O yamnet.tflite
```

## Class Mapping

We map YAMNet's 521 classes to cough categories:
- Class 36 ("Cough") -> cough_detected
- Class 37 ("Throat clearing") -> contributes to cough score
- Class 31 ("Wheeze") -> wet_cough indicator
- Class 30 ("Breathing") -> baseline

## Fallback

If the model file is not present, the classifier falls back to a simple
energy-based heuristic that analyzes audio amplitude patterns.

## Input Format

- PCM float array at 16kHz sample rate
- Frame length: 15600 samples (~0.975 seconds)

## Output

- `CoughResult` with: isCoughDetected, coughType (dry/wet/none), confidence
