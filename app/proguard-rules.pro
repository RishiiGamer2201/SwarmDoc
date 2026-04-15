# SwarmDoc ProGuard Rules

# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Room entities
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.glucodes.swarmdoc.**$$serializer { *; }
-keepclassmembers class com.glucodes.swarmdoc.** { *** Companion; }
-keepclasseswithmembers class com.glucodes.swarmdoc.** { kotlinx.serialization.KSerializer serializer(...); }

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# JTransforms
-keep class pl.edu.icm.jlargearrays.** { *; }
-keep class org.jtransforms.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
