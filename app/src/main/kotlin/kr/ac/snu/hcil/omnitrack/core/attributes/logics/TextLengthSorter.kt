package kr.ac.snu.hcil.omnitrack.core.attributes.logics

/**
 * Created by Young-Ho on 10/12/2017.
 */
class TextLengthSorter(override val name: String, attributeLocalId: String) : AFieldValueSorter(attributeLocalId) {
    override fun compareValues(valueA: Any, valueB: Any): Int {
        return valueA.toString().length.compareTo(valueB.toString().length)
    }
}