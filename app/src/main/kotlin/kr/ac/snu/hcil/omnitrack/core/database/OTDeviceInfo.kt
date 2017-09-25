package kr.ac.snu.hcil.omnitrack.core.database

import com.google.firebase.iid.FirebaseInstanceId
import kr.ac.snu.hcil.omnitrack.BuildConfig

/**
 * Created by Young-Ho on 9/24/2017.
 */
class OTDeviceInfo {
    var os: String? = "Android api-${android.os.Build.VERSION.SDK_INT}"
    var instanceId: String? = FirebaseInstanceId.getInstance().id
    var firstLoginAt: Long = System.currentTimeMillis()
    var appVersion: String? = BuildConfig.VERSION_NAME
}