# Oberon Browser Proguard Rules

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep NanoHTTPD classes for embedded AgentServer
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**

# Keep WebKit and JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# AndroidX Core & Material
-keep class androidx.core.content.FileProvider { *; }
-dontwarn androidx.**
