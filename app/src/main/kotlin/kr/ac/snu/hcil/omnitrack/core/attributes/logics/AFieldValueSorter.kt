package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemDAO

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
abstract class AFieldValueSorter(val attributeLocalId: String) : ItemComparator() {

    abstract fun compareValues(valueA: Any, valueB: Any): Int

    override fun increasingCompare(a: OTItemDAO?, b: OTItemDAO?): Int {
        val valueA = a?.getValueOf(attributeLocalId)
        val valueB = b?.getValueOf(attributeLocalId)

        return if (valueA != null && valueB != null) {
            return compareValues(valueA, valueB)
        } else {
            if (valueA != null) {
                1
            } else if (valueB != null) {
                -1
            } else 0
        }
    }
}