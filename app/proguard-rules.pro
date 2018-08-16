# Add attribute specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/younghokim/Library/Android/sdk/sources/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any attribute specific keep options here:

# If your attribute uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes Signature, *Annotation*

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepnames class * extends kr.ac.snu.hcil.omnitrack.ui.activities.OTFragment

-dontwarn okio.**

# okhttp3
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Log4j
-dontwarn org.apache.log4j.**
-dontnote org.apache.log4j.**

-dontwarn org.apache.commons.math3.**
-dontwarn java.beans.**
-dontwarn org.apache.commons.beanutils.**
-dontwarn org.apache.commons.lang3.**
-dontwarn org.apache.commons.collections.**


-dontwarn io.nlopez.smartlocation.rx.**

-dontwarn sun.misc.**

# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**


# Don't note duplicate definition (Legacy Apche Http Client)
-dontnote android.net.http.*
-dontnote org.apache.http.**


# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8

-dontwarn retrofit.**
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

# Glide settings
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

#For CameraKit >= 0.13.0
-dontwarn com.google.android.gms.**

-keep public class com.google.android.gms.location.DetectedActivity
-dontwarn com.google.android.gms.location.DetectedActivity

-dontwarn com.google.errorprone.annotations.*
-dontwarn com.beloo.widget.chipslayoutmanager.Orientation

-dontwarn kr.ac.snu.hcil.omnitrack.core.visualization.models.DurationHeatMapModel

-dontwarn com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
-dontwarn io.reactivex.functions.Function
-dontwarn rx.internal.util.**
-dontwarn sun.misc.Unsafe

-dontwarn androidx.work.impl.background.systemjob.SystemJobService

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
# end Glide settings