package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber
import java.util.*

/**
 * Created by younghokim on 2017. 11. 14..
 */
class StartOfDayRelativeExpression : Expression.LazyFunction(ExpressionConstants.COMMAND_TIME_START_OF_DAY_RELATIVE, 1) {
    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val offset = lazyParams?.get(0)?.eval()?.toInt() ?: 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, offset) // offset from today

        return LongLazyNumber(cal.timeInMillis)
    }

}