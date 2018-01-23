package kr.ac.snu.hcil.omnitrack.core.di.configured

import com.udojava.evalex.Expression
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.expressions.*
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.Configured

/**
 * Created by younghokim on 2017. 11. 14..
 */
@Module(includes = [ConfiguredModule::class])
class ScriptingModule {
    @Provides
    @Configured
    fun provideSupportedFunctions(configuredContext: ConfiguredContext): Array<Expression.LazyFunction> {
        return arrayOf(
                TodayLogCountExpression(configuredContext),
                DailyLogCountExpression(configuredContext),
                LatestItemTimestampExpression(configuredContext),
                StartOfDayRelativeExpression(),
                IsWeekendExpression()
        )
    }
}