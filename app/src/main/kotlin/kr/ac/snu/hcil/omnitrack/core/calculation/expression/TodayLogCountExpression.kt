package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import dagger.Lazy
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import com.udojava.evalex.Expression
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by Young-Ho on 11/14/2017.
 */
class TodayLogCountExpression(app: OTApp): Expression.LazyFunction(ExpressionConstants.COMMAND_TRACKER_LOG_COUNT_TODAY, 1){
    @Inject lateinit var dbManager: Lazy<RealmDatabaseManager>
    @Inject lateinit var realmProvider: Provider<Realm>

    init{
        app.applicationComponent.inject(this)
    }

    override fun lazyEval(lazyParams: MutableList<Expression.LazyNumber>?): Expression.LazyNumber {
        val trackerId = lazyParams?.get(0)?.string
        if(trackerId != null)
        {
            println("extracted tracker id: ${trackerId}")
            val realm = realmProvider.get()
            val todayItemCount = dbManager.get().makeItemsQueryOfToday(trackerId, realm).count()
            realm.close()
            return LongLazyNumber(todayItemCount)
        }
        else return ExpressionConstants.EXPRESSION_ZERO
    }
}