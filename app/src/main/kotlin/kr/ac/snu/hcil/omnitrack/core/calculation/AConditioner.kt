package kr.ac.snu.hcil.omnitrack.core.calculation

import kr.ac.snu.hcil.omnitrack.utils.serialization.ATypedQueueSerializable

/**
 * Created by younghokim on 16. 9. 5..
 */
abstract class AConditioner : ATypedQueueSerializable {

    companion object {
        const val TYPECODE_SINGLE_NUMERIC_COMPARISON = 0


        fun makeInstance(typeCode: Int): AConditioner? {
            return when (typeCode) {
                TYPECODE_SINGLE_NUMERIC_COMPARISON -> SingleNumericComparison()
                else -> null
            }
        }

        fun makeInstance(typeCode: Int, serialized: String): AConditioner? {
            return when (typeCode) {
                TYPECODE_SINGLE_NUMERIC_COMPARISON -> {
                    val result = SingleNumericComparison()
                    result.fromSerializedString(serialized)
                    result
                }
                else -> null
            }
        }
    }


    abstract val typeCode: Int

    constructor()

    constructor(serialized: String) : super(serialized)

    abstract fun validate(value: Any): Boolean

}