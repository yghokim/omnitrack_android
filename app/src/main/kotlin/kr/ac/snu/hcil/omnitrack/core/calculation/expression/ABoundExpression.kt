package kr.ac.snu.hcil.omnitrack.core.calculation.expression

import javax.annotation.RegEx

/**
 * Created by Young-Ho on 11/14/2017.
 */
abstract class ABoundExpression(@RegEx val regex: String) {
    abstract fun getResultType(): ExpressionResultType
    abstract fun evaluate(match: String): Any
}