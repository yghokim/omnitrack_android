package kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory

/**
 * Created by younghokim on 16. 7. 28..
 */
class MicrosoftBandHeartRateFactory : OTMeasureFactory() {
    override val nameResourceId: Int = R.string.measure_microsoft_band_heart_rate_name
    override val descResourceId: Int = R.string.measure_microsoft_band_heart_rate_desc
}