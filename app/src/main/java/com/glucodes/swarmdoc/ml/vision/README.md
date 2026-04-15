# Vision Models

## MobileNetV3 for Anemia Eye Check

### Current State
The app ships with a **placeholder** MobileNetV3 Small model pre-trained on ImageNet.
This placeholder outputs ImageNet object classes, not medical results.

### Replacing with Real Anemia Model

To swap in a real anemia classification model:

1. Train a MobileNetV3 Small model on labeled eyelid pallor images
   (classification: Normal, Mild Anemia, Moderate Anemia, Severe Anemia)
2. Convert to TFLite: `converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_dir)`
3. Quantize (optional): `converter.optimizations = [tf.lite.Optimize.DEFAULT]`
4. Save as `anemia_mobilenetv3.tflite`
5. Place at: `app/src/main/assets/model/anemia_mobilenetv3.tflite`
6. Update the class labels array in `MobileNetV3Wrapper.kt`

### Placeholder Model Download
- Visit: https://tfhub.dev/google/lite-model/mobilenet_v3_small_100_224/feature_vector/5/metadata/1
- Download the .tflite file (~4MB)
- Place at: `app/src/main/assets/model/mobilenetv3_small.tflite`

### Input Format
- Bitmap resized to 224x224
- Normalized to [0, 1] float range
- RGB channel order

### Pipeline
1. CameraX captures image
2. Image is cropped to lower eyelid region (guided by UI overlay)
3. Preprocessed to 224x224, normalized
4. TFLite Interpreter runs inference
5. Output class scores are mapped to anemia severity

## rPPG Heart Rate

The rPPG processor uses real CameraX ImageAnalysis with:
- ML Kit face detection (offline, bundled)
- Forehead region cropping (top 30% of face bounding box)
- Green channel mean extraction per frame over 15 seconds at 30fps
- IIR bandpass filter (0.75Hz - 3.5Hz)
- Pure Kotlin DFT for peak frequency detection
- Frequency to BPM conversion

No external model file needed - this is a pure signal processing implementation.
