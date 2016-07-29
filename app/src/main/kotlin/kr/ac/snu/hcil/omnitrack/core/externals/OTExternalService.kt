package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band.MicrosoftBandService
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val identifier: String, val minimumSDK: Int) : INameDescriptionResourceProvider {
    companion object {
        fun getAvailableServices(): Array<OTExternalService> {
            return arrayOf(MicrosoftBandService)
        }
    }

    abstract var permissionGranted: Boolean
        protected set

    protected val _measureFactories = ArrayList<OTMeasureFactory<out Any>>()

    val measureFactories: List<OTMeasureFactory<out Any>> get() {
        return _measureFactories
    }

    abstract fun isConnected(): Boolean

    abstract fun isConnecting(): Boolean

    abstract fun connectAsync(connectedHandler: ((Boolean)->Unit)?=null)
    abstract fun disconnect()

    val connected = Event<Any>()
    val disconnected = Event<Any>()


    abstract fun grantPermissions(activity: Activity, handler: ((Boolean) -> Unit)? = null)

}