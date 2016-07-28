package kr.ac.snu.hcil.omnitrack.core.externals

import kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band.MicrosoftBandService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.INameDescriptionResourceProvider
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
abstract class OTExternalService(val identifier: String) : INameDescriptionResourceProvider {
    companion object {
        fun getAvailableServices(): Array<OTExternalService> {
            return arrayOf(MicrosoftBandService())
        }
    }

    protected val _measureFactories = ArrayList<OTMeasureFactory>()

    val measureFactories: List<OTMeasureFactory> get() {
        return _measureFactories
    }

    abstract fun isConnected(): Boolean

    abstract fun isConnecting(): Boolean

    abstract fun connectAsync()
    abstract fun disconnect()

    val connected = Event<Any>()
    val disconnected = Event<Any>()

}