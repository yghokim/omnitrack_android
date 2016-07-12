package kr.ac.snu.hcil.omnitrack.core.attributes

import kr.ac.snu.hcil.omnitrack.core.OTAttribute
import kotlin.properties.Delegates

/**
 * Created by Young-Ho Kim on 2016-07-11.
 */

class OTNumberAttribute : OTAttribute(){
    override val layoutId: Int
        get() = throw UnsupportedOperationException()

    companion object{
        val UNIT = 0
        val DECIMAL_POINTS: String = "decimal_point"
    }

    val unitText by Delegates.observable(""){
        prop, old, new ->
            notifyPropertyChanged(UNIT, old, new)
    }
}