package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import com.udojava.evalex.Expression
import java.math.BigDecimal

/**
 * Created by younghokim on 2017. 11. 14..
 */
class LongLazyNumber(val longValue: Long): Expression.LazyNumber {
    override fun eval(): BigDecimal {
        return BigDecimal(longValue)
    }

    override fun getString(): String {
        return longValue.toString()
    }
}