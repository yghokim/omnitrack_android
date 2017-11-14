package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import com.udojava.evalex.Expression
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants.Companion.EXPRESSION_ZERO
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan

/**
 * Created by younghokim on 2017. 11. 14..
 */
class LatestItemTimestampExpression(app: OTApp) : RealmLazyFunction(app, ExpressionConstants.COMMAND_TRACKER_LATEST_ITEM_TIME, 2) {

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        realmProvider.get().use { realm ->
            val trackerId = lazyParams?.get(0)?.string
            val queryBase = dbManager.get().makeItemsQuery(trackerId, null, null, realm)

            val overrideAttributeLocalId = lazyParams?.get(1)?.string
            if (overrideAttributeLocalId == null) {
                return LongLazyNumber(queryBase.max(RealmDatabaseManager.FIELD_TIMESTAMP_LONG)?.toLong() ?: 0)
            } else {
                val latestItem = queryBase.findAll().maxBy {
                    val value = it.getValueOf(overrideAttributeLocalId)
                    if (value is TimePoint) {
                        return@maxBy value.timestamp
                    } else if (value is TimeSpan) {
                        return@maxBy value.to
                    } else return@maxBy 0L
                }

                if (latestItem == null) {
                    return EXPRESSION_ZERO
                } else {
                    val value = latestItem.getValueOf(overrideAttributeLocalId)
                    return LongLazyNumber(if (value is TimePoint) {
                        value.timestamp
                    } else if (value is TimeSpan) {
                        value.to
                    } else 0L)
                }
            }
        }
    }

}