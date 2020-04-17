package kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions

import android.content.Context
import com.udojava.evalex.Expression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionConstants
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.LongLazyNumber
import java.util.*

/**
 * Created by Young-Ho on 11/14/2017.
 */
class TodayLogCountWithFieldValueExpression(context: Context) : RealmLazyFunction(context, ExpressionConstants.COMMAND_TRACKER_LOG_COUNT_TODAY_WITH_FIELD_VALUE, 3) {

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val trackerId = lazyParams?.get(0)?.string
        val fieldLocalId = lazyParams?.get(1)?.string
        val fieldValue = lazyParams?.get(2)?.string

        if (trackerId != null && fieldLocalId != null && fieldValue != null) {
            val realm = realmProvider.get()
            val itemsWithField = dbManager.get()
                    .makeItemsQueryOfTheDay(trackerId, realm)
                    .equalTo("fieldValueEntries.key", fieldLocalId)
                    .findAll()

            val filteredItemCount = itemsWithField.count { item ->

                val value = item.getValueOf(fieldLocalId)
                if (fieldValue.toLowerCase(Locale.ROOT) == "null" && value == null) {
                    return@count true
                } else if (value is IntArray) {
                    value.sort()
                    val split = fieldValue.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                    split.sort()
                    return@count value.contentEquals(split)
                } else return@count value.toString() == fieldValue
            }

            realm.close()

            return LongLazyNumber(filteredItemCount.toLong())
        } else return ExpressionConstants.EXPRESSION_ZERO
    }
}