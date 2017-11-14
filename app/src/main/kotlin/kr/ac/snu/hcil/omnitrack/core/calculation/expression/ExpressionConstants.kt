package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import com.udojava.evalex.Expression
import java.math.BigDecimal
import javax.annotation.RegEx

/**
 * Created by Young-Ho on 11/14/2017.
 */
class ExpressionConstants {
    companion object {
        const val OPERATOR_ARITH_ADD = "+"
        const val OPERATOR_ARITH_SUB = "-"
        const val OPERATOR_ARITH_MULTIPLY = "*"

        const val OPERATOR_LOGICAL_AND = "&&"
        const val OPERATOR_LOGICAL_OR = "||"

        const val OPERATOR_RELATIONAL_EQUALS = "=="
        const val OPERATOR_RELATIONAL_DIFFERENT = "!="

        const val COMMAND_TRACKER = "tracker"
        const val COMMAND_TRACKER_LOG_COUNT_TODAY = "todayLogCountOf"

        val EXPRESSION_ZERO: Expression.LazyNumber by lazy{
            object: Expression.LazyNumber{
                override fun eval(): BigDecimal {
                    return BigDecimal.ZERO
                }

                override fun getString(): String {
                    return "0"
                }

            }
        }
    }
}