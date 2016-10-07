package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute

/**
 * Created by Young-Ho Kim on 2016-09-07
 */
class AttributeSorter(val attribute: OTAttribute<out Any>) : ItemComparator() {
    override val name: String get() = attribute.name

    override fun increasingCompare(a: OTItem, b: OTItem): Int {
        val valueA = a.getValueOf(attribute)
        val valueB = b.getValueOf(attribute)

        return if (valueA != null && valueB != null) {
            return attribute.compareValues(valueA, valueB)
        } else {
            if (valueA != null) {
                1
            } else if (valueB != null) {
                -1
            } else 0
        }
    }
}