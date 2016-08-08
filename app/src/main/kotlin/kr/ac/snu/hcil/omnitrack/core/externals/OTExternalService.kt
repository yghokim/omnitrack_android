package kr.ac.snu.hcil.omnitrack.core.externals

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import kr.ac.snu.hcil.omnitrack.core.externals.device.AndroidDeviceService
import kr.ac.snu.hcil.omnitrack.core.externals.google.fit.GoogleFitService
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
            arrayOf(AndroidDeviceService, GoogleFitService, MicrosoftBandService, MiBandService)
        }
    }

    enum class ServiceState {
        DEACTIVATED, ACTIVATING, ACTIVATED
    }

    abstract val permissionGranted: Boolean

    protected val _measureFactories = ArrayList<OTMeasureFactory<out Any>>()

    val measureFactories: List<OTMeasureFactory<out Any>> get() {
        return _measureFactories
    }

    abstract fun getState(): ServiceState

    abstract fun activateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)? = null)
    abstract fun deactivate()

    val activated = Event<Any>()
    val deactivated = Event<Any>()

    abstract val thumbResourceId: Int

    abstract fun grantPermissions(caller: Fragment, requestCode: Int)
    abstract fun grantPermissions(caller: Activity, requestCode: Int)

    abstract fun prepareService()

}