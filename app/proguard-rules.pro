# Keep application classes and BuildConfig values used by the UI.
-keep class com.alvand.salonmanager.** { *; }

# Preserve useful crash report information.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose and Kotlin libraries ship their own consumer rules. Avoid noisy
# warnings from optional annotations used by those libraries.
-dontwarn org.jetbrains.annotations.**
