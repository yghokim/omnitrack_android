package kr.ac.snu.hcil.omnitrack.core.datatypes

/**
 * Created by younghokim on 2017. 11. 16..
 */
class Fraction {
    companion object {
        const val EPSILON: Float = 0.0001f

        fun fromRatioAndUnder(ratio: Float, under: Short): Fraction {
            return Fraction(Math.round(ratio * under).toShort(), under)
        }

        fun softEquals(a: Fraction, b: Fraction): Boolean {
            return Math.abs(a.toFloat() - b.toFloat()) < EPSILON
        }
    }

    var upper: Short = 0
    var under: Short = 1

    constructor(upper: Short, under: Short) {
        this.upper = upper
        this.under = under
    }

    fun toDouble(): Double = upper.toDouble() / under
    fun toFloat(): Float = upper.toFloat() / under

    override fun equals(other: Any?): Boolean {
        return if (other is Fraction) {
            other.upper == upper && other.under == under
        } else false
    }

    override fun toString(): String {
        return "Fraction(upper:$upper, under: $under, float: ${toFloat()})"
    }
}