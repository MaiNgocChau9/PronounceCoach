# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep data classes for serialization
-keep class com.pronouncecoach.android.data.** { *; }
-keep class com.pronouncecoach.android.scoring.** { *; }
