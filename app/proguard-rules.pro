# Keep Room entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Gson models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.jarvis.assistant.ai.** { *; }
-keep class com.jarvis.assistant.memory.** { *; }
