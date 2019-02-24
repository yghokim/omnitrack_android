package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import android.content.Context
import com.udojava.evalex.Expression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber

/**
 * Created by Young-Ho on 11/14/2017.
 */
class TodayLogCountExpression(context: Context) : RealmLazyFunction(context, ExpressionConstants.COMMAND_TRACKER_LOG_COUNT_TODAY, 1) {

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val trackerId = lazyParams?.get(0)?.string
        if(trackerId != null)
        {
            println("extracted tracker id: $trackerId")
            val realm = realmProvider.get()
            val todayItemCount = dbManager.get().makeItemsQueryOfTheDay(trackerId, realm).count()
            realm.close()
            return LongLazyNumber(todayItemCount)
        }
        else return ExpressionConstants.EXPRESSION_ZERO
    }
}