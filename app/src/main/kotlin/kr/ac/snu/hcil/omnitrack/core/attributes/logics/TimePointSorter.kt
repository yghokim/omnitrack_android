package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint

/**
 * Created by Young-Ho on 10/12/2017.
 */
class TimePointSorter(override val name: String, attributeLocalId: String) : AFieldValueSorter(attributeLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        return if (valueA is TimePoint && valueB is TimePoint) {
            valueA.timestamp.compareTo(valueB.timestamp)
        } else 0
    }
}