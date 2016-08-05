package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import kr.ac.snu.hcil.omnitrack.core.externals.device.AndroidDeviceService
import kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band.MicrosoftBandService
import kr.ac.snu.hcil.omnitrack.core.externals.shaomi.miband.MiBandService
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val identifier: String, val minimumSDK: Int) : INameDescriptionResourceProvider {
    companion object {
        val availableServices: Array<OTExternalService> by lazy {
            arrayOf(AndroidDeviceService, MicrosoftBandService, MiBandService)
        }
    }

    abstract var permissionGranted: Boolean
        protected set

    protected val _measureFactories = ArrayList<OTMeasureFactory<out Any>>()

    val measureFactories: List<OTMeasureFactory<out Any>> get() {
        return _measureFactories
    }

    abstract fun isActivated(): Boolean

    abstract fun isActivating(): Boolean

    abstract fun activateAsync(connectedHandler: ((Boolean) -> Unit)? = null)
    abstract fun deactivate()

    val activated = Event<Any>()
    val deactivated = Event<Any>()

    abstract fun grantPermissions(activity: Activity, handler: ((Boolean) -> Unit)? = null)

}