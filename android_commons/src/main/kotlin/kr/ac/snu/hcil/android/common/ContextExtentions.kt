package kr.ac.snu.hcil.android.common

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

/**
 * Created by younghokim on 16. 7. 28..
 */

const val ANDROID_ASSET_PATH = "file:///android_asset"

fun Context.versionName(): String {
    val pInfo = packageManager.getPackageInfo(packageName, 0)
    return pInfo.versionName
}


fun Context.versionCode(): Long {
    val pInfo = packageManager.getPackageInfo(packageName, 0)
    return if (Build.VERSION.SDK_INT >= 28) {
        pInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        pInfo.versionCode.toLong()
    }
}

fun Context.getActionPrefix(): String {
    return "${this.packageName}.action"
}

@TargetApi(23)
fun isInDozeMode(context: Context): Boolean {
    return (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isDeviceIdleMode
}

fun dipSize(context: Context, value: Float): Float {
    return value * context.resources.displayMetrics.density
}

fun dipSize(context: Context, value: Int): Float {
    return value * context.resources.displayMetrics.density
}

val PowerManager.isInteractiveCompat: Boolean
    get() {

        if (Build.VERSION.SDK_INT <= 19) {
            @Suppress("DEPRECATION")
            return this.isScreenOn
        } else {
            return this.isInteractive
        }
    }
val KeyguardManager.isDeviceLockedCompat: Boolean
    get() {
        if (Build.VERSION.SDK_INT < 22) {
            return this.isKeyguardLocked
        } else {
            return this.isDeviceLocked
        }
    }