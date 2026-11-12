# ARTGG Mouse Pro — ProGuard/R8 Rules

# Keep Kotlin serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models used with Room + kotlinx.serialization
-keep class com.artgg.mousepro.data.model.** { *; }
-keep class com.artgg.mousepro.data.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Shizuku API (optional dependency)
-dontwarn rikka.shizuku.**
-keep class rikka.shizuku.** { *; }

# Accessibility service must not be obfuscated
-keep class com.artgg.mousepro.service.ArtggAccessibilityService { *; }
-keep class com.artgg.mousepro.service.InputCaptureService { *; }

# Compose
-dontwarn androidx.compose.**