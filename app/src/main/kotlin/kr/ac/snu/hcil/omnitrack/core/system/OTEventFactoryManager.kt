package kr.ac.snu.hcil.omnitrack.core.system

import android.content.Context
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory
import kr.ac.snu.hcil.omnitrack.core.event.device.OTLocationProximityEventFactory
import kr.ac.snu.hcil.omnitrack.core.event.device.OTPhoneCallEventFactory
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

class OTEventFactoryManager(val context: Context){

    val installedFactories: Array<OTEventFactory> by lazy {
        arrayOf(
                OTLocationProximityEventFactory(context),
                OTPhoneCallEventFactory(context)
        )
    }

    val availableFactories: List<OTEventFactory>
        @Synchronized get()  = installedFactories.toList()

    fun getFactoryByCode(typeCode: String): OTEventFactory? {
        return availableFactories.find{ it.typeCode === typeCode }
    }
}