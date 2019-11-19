# Add field specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/younghokim/Library/Android/sdk/sources/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any field specific keep options here:

# If your field uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

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


#Retrofit2#################################################################
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.-KotlinExtensions

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

#end Retrofit2#################################################################

#For CameraKit >= 0.13.0
-dontwarn com.google.android.gms.**

-keep public class com.google.android.gms.location.DetectedActivity
-dontwarn com.google.android.gms.location.DetectedActivity

-dontwarn com.google.errorprone.annotations.*

-dontwarn kr.ac.snu.hcil.omnitrack.core.visualization.models.DurationHeatMapModel

-dontwarn io.reactivex.functions.Function
-dontwarn rx.internal.util.**
-dontwarn sun.misc.Unsafe

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
# end Glide settings

# R8 Configuration for Realm
-keep @interface io.realm.annotations.RealmModule { *; }
-keep class io.realm.annotations.RealmModule { *; }

-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class *
-dontwarn javax.**
-keepnames public class * extends io.realm.RealmObject
-keep public class * extends io.realm.RealmObject { *; }
-keep class io.realm.** { *; }