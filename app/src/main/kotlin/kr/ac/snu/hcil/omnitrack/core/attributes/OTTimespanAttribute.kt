package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.OTSettingProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.Timespan

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTTimespanAttribute() : OTAttribute() {
    override val layoutId: Int
        get() = throw UnsupportedOperationException()
    override val settingsProperties: Array<OTSettingProperty<Any>>
        get() = throw UnsupportedOperationException()

}