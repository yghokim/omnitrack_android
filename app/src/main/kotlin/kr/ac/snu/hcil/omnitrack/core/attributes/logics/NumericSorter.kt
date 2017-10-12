package kr.ac.snu.hcil.omnitrack.core.attributes.logics

import kr.ac.snu.hcil.omnitrack.utils.toBigDecimal

/**
 * Created by Young-Ho on 10/12/2017.
 */
class NumericSorter(override val name: String, attributeLocalId: String) : AFieldValueSorter(attributeLocalId) {

    override fun compareValues(valueA: Any, valueB: Any): Int {
        return try {
            val bdA = toBigDecimal(valueA)
            val bdB = toBigDecimal(valueB)
            bdA.compareTo(bdB)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

}