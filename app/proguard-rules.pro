# R8 / ProGuard rules for the RecapFlowAI release build.
#
# Release enables minify + resource shrinking, so anything reached only through JNI or by name
# has to be kept explicitly. The rules below are the ones this app actually needs; the rest of
# the file deliberately stays empty so R8 can work.

# ---------------------------------------------------------------------------
# JNI boundary
# ---------------------------------------------------------------------------
# recapflow_jni.cpp resolves these by name, so obfuscating or merging them breaks the bridge at
# runtime with a silent LinkageError rather than a build error.
#
#   FindClass("com/recapflow/ai/media/NativeProbePayload")
#   GetMethodID(payload, "<init>", "(IILjava/lang/String;...;)V")
-keep class com.recapflow.ai.media.NativeProbePayload { *; }

# nativeVersionFromJni() and probeFromJni() are called from C++ by their mangled Kotlin names.
-keepclasseswithmembernames,includedescriptorclasses class com.recapflow.ai.media.NativeMediaBridge {
    native <methods>;
}

# ---------------------------------------------------------------------------
# Kotlin metadata used by the app at runtime
# ---------------------------------------------------------------------------
# EditorPreferencesStore and the media enums are read by name in a few places, and keeping the
# enum valueOf/values pair avoids surprises if a lookup by name is added later.
-keepclassmembers enum com.recapflow.ai.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the line numbers so release crash reports stay readable, but hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Diagnostics that should survive shrinking
# ---------------------------------------------------------------------------
# The Compose/ViewBinding generated classes are referenced from XML by name.
-keep class com.recapflow.ai.databinding.** { *; }

# Media3 effect and transformer implementations are instantiated through generated factories.
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**
