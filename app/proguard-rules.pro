# Keep application models and data classes
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-keep class androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Koin
-keep class io.insertkoin.** { *; }

# Coil
-dontwarn coil.**
