# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.dologan.humblebrowser.data.api.** { *; }

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
