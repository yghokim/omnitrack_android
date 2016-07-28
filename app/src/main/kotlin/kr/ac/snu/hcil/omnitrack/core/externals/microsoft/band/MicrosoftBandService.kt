package kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 7. 28..
 */
class MicrosoftBandService : OTExternalService("MicrosoftBandService") {

    override val nameResourceId: Int = R.string.service_microsoft_band_name
    override val descResourceId: Int = R.string.service_microsoft_band_desc

    init {
        _measureFactories += MicrosoftBandHeartRateFactory()
    }

    override fun isConnected(): Boolean {
        return false
    }

    override fun isConnecting(): Boolean {
        return false
    }

    override fun connectAsync() {

    }

    override fun disconnect() {

    }


}