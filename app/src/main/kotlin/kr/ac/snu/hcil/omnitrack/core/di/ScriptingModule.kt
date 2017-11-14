package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import com.udojava.evalex.Expression
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.DailyLogCountExpression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.LatestItemTimestampExpression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.StartOfDayRelativeExpression
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.TodayLogCountExpression
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 14..
 */
@Module(includes = arrayOf(ApplicationModule::class, BackendDatabaseModule::class))
class ScriptingModule {
    @Provides
    @Singleton
    fun provideSupportedFunctions(context: Context): Array<Expression.LazyFunction> {
        return arrayOf(
                TodayLogCountExpression(context.applicationContext as OTApp),
                DailyLogCountExpression(context.applicationContext as OTApp),
                LatestItemTimestampExpression(context.applicationContext as OTApp),
                StartOfDayRelativeExpression()
        )
    }
}