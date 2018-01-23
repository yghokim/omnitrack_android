package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import com.udojava.evalex.Expression
import java.math.BigDecimal

/**
 * Created by Young-Ho on 11/14/2017.
 */
class ExpressionConstants {
    companion object {

        const val COMMAND_TRACKER_LOG_COUNT_DAILY = "dailyLogCountOf"
        const val COMMAND_TRACKER_LOG_COUNT_TODAY = "todayLogCountOf"
        const val COMMAND_TRACKER_LATEST_ITEM_TIME = "latestItemTimeOf"
        const val COMMAND_TIME_START_OF_DAY_RELATIVE = "startOfDayRelative"
        const val COMMAND_IS_WEEKEND = "isWeekend"

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