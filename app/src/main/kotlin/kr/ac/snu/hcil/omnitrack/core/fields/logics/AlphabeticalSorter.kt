package kr.ac.snu.hcil.omnitrack.core.fields.logics

/**
 * Created by Young-Ho on 10/12/2017.
 */
class AlphabeticalSorter(override val name: String, fieldLocalId: String) : AFieldValueSorter(fieldLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        return valueA.toString().compareTo(valueB.toString())
    }

}