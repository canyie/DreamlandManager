# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute ProGuard

-printmapping mapping.txt

-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable

-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.canyie.dreamland.manager.ipc.** { *; }
-keep class com.google.android.material.** { *; }
-keep class androidx.** { *; }
-keep class android.support.** { *; }
-keep class * extends androidx.**
-keep interface androidx.** { *; }
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
