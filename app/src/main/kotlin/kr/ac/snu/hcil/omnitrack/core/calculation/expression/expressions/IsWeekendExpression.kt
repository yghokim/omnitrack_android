package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import kr.ac.snu.hcil.android.common.time.getDayOfWeek
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber
import java.util.*

/**
 * Created by younghokim on 2017. 11. 14..
 */
class IsWeekendExpression : Expression.LazyFunction(ExpressionConstants.COMMAND_IS_WEEKEND, 0, true) {

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val cal = Calendar.getInstance()
        val dow = cal.getDayOfWeek()
        return LongLazyNumber(if (dow == 1 || dow == 7) {
            1
        } else 0)
    }

}