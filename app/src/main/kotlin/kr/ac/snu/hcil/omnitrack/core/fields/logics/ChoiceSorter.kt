package kr.ac.snu.hcil.omnitrack.core.fields.logics

import kr.ac.snu.hcil.android.common.containers.UniqueStringEntryList

/**
 * Created by Young-Ho on 10/12/2017.
 */
class ChoiceSorter(override val name: String, val entries: UniqueStringEntryList, fieldLocalId: String) : AFieldValueSorter(fieldLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        if (valueA is IntArray && valueB is IntArray) {
            if (valueA.size > 0 && valueB.size > 0) {
                val idA = valueA.first()
                val idB = valueB.first()

                val valueA = entries.findWithId(idA)
                val valueB = entries.findWithId(idB)
                if (valueA != null && valueB != null) {
                    println("$valueA compare $valueB :  ${valueA.text.compareTo(valueB.text)}")
                    return valueA.text.compareTo(valueB.text)
                } else if (valueA == null) {
                    return -1
                } else if (valueB == null) {
                    return 1
                } else return 0

            } else if (valueA.size == 0) {
                return -1
            } else if (valueB.size == 0) {
                return 1
            } else return 0
        }
        return 0
    }
}