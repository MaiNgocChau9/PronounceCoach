# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes for serialization
-keep class com.openpronounce.android.data.** { *; }
-keep class com.openpronounce.android.scoring.** { *; }
