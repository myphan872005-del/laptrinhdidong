# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.ued.custommaps.models.** { *; }

# Keep OSMDroid
-dontwarn org.osmdroid.**
-keep class org.osmdroid.** { *; }
