package kr.ac.snu.hcil.omnitrack.core.fields.logics

import kr.ac.snu.hcil.omnitrack.core.types.TimeSpan

/**
 * Created by Young-Ho on 10/12/2017.
 */
class TimeSpanIntervalSorter(override val name: String, fieldLocalId: String) : AFieldValueSorter(fieldLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        return if (valueA is TimeSpan && valueB is TimeSpan) {
            valueA.duration.compareTo(valueB.duration)
        } else 0
    }
}