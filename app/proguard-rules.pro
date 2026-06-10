# Keep source file names and line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable

# Keep Kotlin metadata
-keepattributes *Annotation*, Signature, Exceptions

# Kotlin serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ACRA
-keep class org.acra.** { *; }

# Hilt
-dontwarn com.google.dagger.**
-keep class com.google.dagger.** { *; }

# Timber – keep class names for logging
-keep class com.jakewharton.timber.** { *; }
-keep class * extends com.jakewharton.timber.Timber$Tree { *; }
