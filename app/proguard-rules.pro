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
#-renamesourcefileattribute SourceFile

# WorkManager rules for Android 12+ compatibility
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Hilt rules to prevent obfuscation of generated classes
-keep class ca.unb.mobiledev.handyhub.Hilt_HandyHubApplication { *; }
-keep class ca.unb.mobiledev.handyhub.Hilt_HandyHubApplication$** { *; }
-keep class ca.unb.mobiledev.handyhub.Hilt_HandyHubApplication$* { *; }
-keep class * extends ca.unb.mobiledev.handyhub.Hilt_HandyHubApplication { *; }
-keep class ca.unb.mobiledev.handyhub.Hilt_HandyHubApplication extends android.app.Application { *; }