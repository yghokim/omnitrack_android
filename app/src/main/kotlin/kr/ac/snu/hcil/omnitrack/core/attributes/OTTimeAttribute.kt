package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.OTProperty
import kr.ac.snu.hcil.omnitrack.core.datatypes.Timespan

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */
class OTTimeAttribute() : OTAttribute() {
    override val layoutId: Int
        get() = throw UnsupportedOperationException()
    override val settingsProperties: Array<OTProperty<Any>>
        get() = throw UnsupportedOperationException()

}