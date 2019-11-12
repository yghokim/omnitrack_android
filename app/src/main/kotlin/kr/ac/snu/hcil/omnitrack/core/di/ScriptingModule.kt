package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import com.udojava.evalex.Expression
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.*
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 14..
 */
@Module(includes = [ApplicationModule::class])
class ScriptingModule {
    @Provides
    @Singleton
    fun provideSupportedFunctions(context: Context): Array<Expression.LazyFunction> {
        return arrayOf(
                TodayLogCountExpression(context),
                DailyLogCountExpression(context),
                LatestItemTimestampExpression(context),
                StartOfDayRelativeExpression(),
                IsWeekendExpression()
        )
    }
}