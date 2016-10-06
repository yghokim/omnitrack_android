package kr.ac.snu.hcil.omnitrack.core.calculation

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.convertNumericToDouble
import kr.ac.snu.hcil.omnitrack.utils.isNumericPrimitive
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue

/**
 * Created by younghokim on 16. 9. 5..
 */
class SingleNumericComparison : AConditioner {

    override val typeCode: Int = AConditioner.TYPECODE_SINGLE_NUMERIC_COMPARISON

    enum class ComparisonMethod(val symbol: String, val symbolImageResourceId: Int) {
        /*
        BiggerThan(">", R.drawable.symbol_bigger), BiggerOrEqual("≧", R.drawable.symbol_bigger_or_equal), Equal("=", R.drawable.symbol_equal), SmallerOfEqual("≦", R.drawable.symbol_smaller_or_equal), SmallerThan("<", R.drawable.symbol_smaller)
    */
        BiggerThan(">", R.drawable.icon_threshold_up),
        SmallerThan("<", R.drawable.icon_threshold_down)
    }

    var comparedTo: Double = 10.0

    var method: ComparisonMethod = ComparisonMethod.BiggerThan


    constructor() : super()
    constructor(serialized: String) : super(serialized)

    override fun validate(value: Any): Boolean {
        if (isNumericPrimitive(value)) {
            return when (method) {
            //ComparisonMethod.Equal -> convertNumericToDouble(value) == comparedTo
            //ComparisonMethod.BiggerOrEqual -> convertNumericToDouble(value) >= comparedTo
                ComparisonMethod.BiggerThan -> convertNumericToDouble(value) > comparedTo
            //ComparisonMethod.SmallerOfEqual -> convertNumericToDouble(value) <= comparedTo
                ComparisonMethod.SmallerThan -> convertNumericToDouble(value) < comparedTo
            }
        } else return false
    }


    override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        println("deserialize comparison")
        method = ComparisonMethod.values()[typedQueue.getInt()]
        comparedTo = typedQueue.getDouble()
    }

    override fun onSerialize(typedQueue: SerializableTypedQueue) {
        typedQueue.putInt(method.ordinal)
        typedQueue.putDouble(comparedTo)
    }

    override fun toString(): String {
        return "Single Numeric Comparison: ${method} ${comparedTo}"
    }

}