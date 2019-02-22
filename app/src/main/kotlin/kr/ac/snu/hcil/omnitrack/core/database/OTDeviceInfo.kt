package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import androidx.annotation.Keep
import com.github.salomonbrys.kotson.jsonObject
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.di.global.FirebaseComponent
import kr.ac.snu.hcil.omnitrack.utils.versionName

/**
 * Created by Young-Ho on 9/24/2017.
 */
@Keep
class OTDeviceInfo private constructor(context: Context) {
    var os: String = OS
    var deviceId: String = (context.applicationContext as OTAndroidApp).deviceId
    var instanceId: String? = null
    var firstLoginAt: Long = System.currentTimeMillis()
    var appVersion: String = context.versionName()

    fun convertToJson(): JsonObject {
        return jsonObject(
                "os" to os,
                "deviceId" to deviceId,
                "instanceId" to instanceId,
                "firstLoginAt" to firstLoginAt,
                "appVersion" to appVersion
        )
    }

    companion object {

        val OS: String get() = "Android api-${android.os.Build.VERSION.SDK_INT}"

        fun makeDeviceInfo(context: Context, firebaseComponent: FirebaseComponent): Single<OTDeviceInfo> {
            return firebaseComponent.getFirebaseInstanceIdToken().map { token ->
                OTDeviceInfo(context).apply {
                    instanceId = token
                }
            }
        }
    }
}