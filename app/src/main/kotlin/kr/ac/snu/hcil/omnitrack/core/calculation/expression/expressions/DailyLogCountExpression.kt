package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext

/**
 * Created by Young-Ho on 11/14/2017.
 */
/**
 * This expression receives 3 arguments.
 * first: trackerId(string)
 * second: offset from today (integer)
 * third: attr Local Id of the TimePoint of TimeSpan column.
 */
class DailyLogCountExpression(configuredContext: ConfiguredContext) : RealmLazyFunction(configuredContext, ExpressionConstants.COMMAND_TRACKER_LOG_COUNT_DAILY, 3) {

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val trackerId = lazyParams?.get(0)?.string
        val offset = lazyParams?.get(1)?.eval()?.toInt()
        val overrideAttributeLocalId = lazyParams?.get(2)?.string

        if (trackerId != null) {
            println("extracted tracker id: $trackerId")
            val realm = realmProvider.get()
            val itemCount = dbManager.get().getItemCountDuring(trackerId, realm, offset ?: 0, overrideAttributeLocalId)
            realm.close()
            return LongLazyNumber(itemCount)
        } else return ExpressionConstants.EXPRESSION_ZERO
    }
}