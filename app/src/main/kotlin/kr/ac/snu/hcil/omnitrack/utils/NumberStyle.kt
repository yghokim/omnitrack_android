package kr.ac.snu.hcil.omnitrack.utils

import org.atteo.evo.inflector.English
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Created by younghokim on 16. 9. 5..
 */
class NumberStyle {

    enum class UnitPosition(val id: Int) {
        Front(0), Rear(1)
    }

    var unitPosition: UnitPosition
        get() {
            return when (unitPositionId) {
                UnitPosition.Rear.id -> UnitPosition.Rear
                UnitPosition.Front.id -> UnitPosition.Front
                else -> UnitPosition.Rear
            }
        }
        set(value) {
            unitPositionId = value.id
        }

    private var unitPositionId: Int = UnitPosition.Rear.id

    var unit: String = ""
    var pluralizeUnit: Boolean = false
    var fractionPart: Int = 0
    var commaUnit: Int = 3

    fun makeDecimalFormat(): DecimalFormat {
        //example: DecimalFormat("#,###.###")

        val formatStringBuilder = StringBuilder().append('#')
        if (commaUnit > 0) {
            formatStringBuilder.append(",")
            for (i in 0..commaUnit - 1) {
                formatStringBuilder.append('#')
            }
        }

        if (fractionPart > 0) {
            formatStringBuilder.append('.')
            for (i in 0..fractionPart - 1) {
                formatStringBuilder.append('#')
            }
        }

        return DecimalFormat(formatStringBuilder.toString())
    }

    fun formatNumber(number: BigDecimal): String {

        val formattedNumber = makeDecimalFormat().format(number)
        if (unit.isNotBlank()) {
            val pluralized = English.plural(unit, Math.ceil(number.toDouble()).toInt())
            when (unitPosition) {
                UnitPosition.Front ->
                    return pluralized + ' ' + formattedNumber
                UnitPosition.Rear ->
                    return formattedNumber + ' ' + pluralized
            }
        } else return formattedNumber
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        } else if (other is NumberStyle) {

            return other.pluralizeUnit == pluralizeUnit &&
                    other.unitPositionId == unitPositionId &&
                    other.unit == unit &&
                    other.fractionPart == fractionPart &&
                    other.commaUnit == commaUnit

        } else return false


    }
}