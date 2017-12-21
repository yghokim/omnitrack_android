package kr.ac.snu.hcil.omnitrack.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.R
import org.atteo.evo.inflector.English
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Created by younghokim on 16. 9. 5..
 */
class NumberStyle {

    companion object {
        val typeAdapter: TypeAdapter<NumberStyle> by lazy {
            NumberStyleTypeAdapter()
        }
    }

    internal class NumberStyleTypeAdapter : TypeAdapter<NumberStyle>() {
        override fun read(input: JsonReader): NumberStyle {


            return NumberStyle().apply {

                input.beginObject()
                while (input.hasNext()) {
                    when (input.nextName()) {
                        "unitPosition" -> this.unitPositionId = input.nextInt()
                        "unit" -> this.unit = input.nextString()
                        "pluralize" -> this.pluralizeUnit = input.nextBoolean()
                        "fraction" -> this.fractionPart = input.nextInt()
                        "comma" -> this.commaUnit = input.nextInt()
                    }
                }
                input.endObject()
            }
        }

        override fun write(out: JsonWriter, value: NumberStyle) {
            out.beginObject()
            out.name("unitPosition").value(value.unitPositionId)
            out.name("unit").value(value.unit)
            out.name("pluralize").value(value.pluralizeUnit)
            out.name("fraction").value(value.fractionPart)
            out.name("comma").value(value.commaUnit)
            out.endObject()
        }
    }

    class FormattedInformation(var unitPart: String?, var numberPart: String, var unitPosition: UnitPosition) {
        val unitPartStart: Int get() {
            return when (unitPosition) {
                UnitPosition.Rear -> numberPart.length + 1
                UnitPosition.Front -> 0
                UnitPosition.None -> 0
            }
        }

        val unitPartEnd: Int get() {
            return unitPartStart + (unitPart?.length ?: 0)
        }

        val numberPartStart: Int get() {
            return when (unitPosition) {
                UnitPosition.Rear -> 0
                UnitPosition.Front -> (unitPart?.length ?: 0) + 1
                UnitPosition.None -> 0
            }
        }

        val numberPartEnd: Int get() {
            return numberPartStart + numberPart.length
        }


    }

    enum class UnitPosition(val nameResId: Int) {
        None(R.string.property_number_style_unit_position_none),
        Front(R.string.property_number_style_unit_position_front),
        Rear(R.string.property_number_style_unit_position_rear)
    }

    var unitPosition: UnitPosition
        get() {
            return UnitPosition.values()[unitPositionId]
        }
        set(value) {
            unitPositionId = value.ordinal
        }

    private var unitPositionId: Int = UnitPosition.None.ordinal

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

        val format = DecimalFormat(formatStringBuilder.toString())

        if (fractionPart == 0) {
            format.roundingMode = RoundingMode.FLOOR
        }

        return format
    }

    fun formatNumber(number: Any, infoOut: FormattedInformation? = null): String {
        infoOut?.unitPosition = UnitPosition.None
        infoOut?.unitPart = null

        if (isNumericPrimitive(number) || number is BigDecimal) {
            val formattedNumber = makeDecimalFormat().format(number)
            infoOut?.numberPart = formattedNumber

            if (unit.isNotBlank() || unitPosition == UnitPosition.None) {

                val pluralized = if (pluralizeUnit) {
                    English.plural(unit, Math.ceil(
                            if (number is Int) number.toDouble()
                            else if (number is Long) number.toDouble()
                            else if (number is Float) number.toDouble()
                            else if (number is Double) number
                            else if (number is BigDecimal) number.toDouble()
                            else 0.0
                    ).toInt())
                } else unit

                infoOut?.unitPart = pluralized
                infoOut?.unitPosition = unitPosition

                when (unitPosition) {
                    UnitPosition.Front ->
                        return pluralized + ' ' + formattedNumber
                    UnitPosition.Rear ->
                        return formattedNumber + ' ' + pluralized
                    UnitPosition.None ->
                        return formattedNumber
                }
            } else return formattedNumber
        } else {
            infoOut?.numberPart = number.toString()
            return number.toString()
        }
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