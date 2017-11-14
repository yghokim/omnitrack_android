package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import com.udojava.evalex.Expression
import java.math.BigDecimal

/**
 * Created by younghokim on 2017. 11. 14..
 */
class ExpressionEvaluator(val expressionString:String, vararg supportedFunctions: Expression.LazyFunction) {
    val expression: Expression
    init{
        expression = Expression(expressionString)
        supportedFunctions.forEach {
            expression.addLazyFunction(it)
        }
    }

    fun eval(): Number{
        return expression.eval()
    }

    fun evalBoolean(): Boolean{
        return expression.eval() != BigDecimal.ZERO
    }
}