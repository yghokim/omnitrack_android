package kr.ac.snu.hcil.omnitrack.core.externals.device

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory

/**
 * Created by younghokim on 16. 8. 4..
 */
object AndroidDeviceService : OTExternalService("AndroidDeviceService", 19) {

    override fun isSupportedInSystem(): Boolean {
        return true
    }

    override fun onDeactivate() {

    }

    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return emptyArray()
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_androiddevice
    override val nameResourceId: Int = R.string.service_device_name
    override val descResourceId: Int = R.string.service_device_desc

}