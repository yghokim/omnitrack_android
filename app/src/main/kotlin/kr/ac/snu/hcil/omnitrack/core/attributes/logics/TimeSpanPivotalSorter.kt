package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan

/**
 * Created by Young-Ho on 10/12/2017.
 */
class TimeSpanPivotalSorter(override val name: String, val start: Boolean, attributeLocalId: String) : AFieldValueSorter(attributeLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        return if (valueA is TimeSpan && valueB is TimeSpan) {
            if (start) {
                valueA.from.compareTo(valueB.from)
            } else {
                valueA.to.compareTo(valueB.to)
            }
        } else 0
    }
}