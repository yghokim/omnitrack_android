package kr.ac.snu.hcil.omnitrack.core.calculation.expression

/**
 * Created by Young-Ho on 11/14/2017.
 */
interface IArithmeticNode: IExpressionNode {
    fun evaluate(): Number
}