# Add project specific ProGuard rules here.
-keep class com.example.receiptscanner.domain.model.** { *; }
-keep class com.example.receiptscanner.data.local.** { *; }

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ML Kit
-keep class com.google.mlkit.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }

# Gemini
-keep class com.google.ai.client.** { *; }
