package kr.ac.snu.hcil.omnitrack.utils

import android.content.Context
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.Fraction

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class RatingOptions {

    companion object {
        val typeAdapter: TypeAdapter<RatingOptions> by lazy {
            RatingOptionsTypeAdapter()
        }
    }

    internal class RatingOptionsTypeAdapter : TypeAdapter<RatingOptions>() {
        override fun read(input: JsonReader): RatingOptions {

            val out = RatingOptions()

            input.beginObject()
            while (input.hasNext()) {
                when (input.nextName()) {
                    "type" -> {
                        out.type = DisplayType.values()[input.nextInt()]
                    }
                    "stars" -> {
                        out.stars = input.nextInt()
                    }
                    "left" -> {
                        out.leftMost = input.nextInt()
                    }
                    "right" -> {
                        out.rightMost = input.nextInt()
                    }
                    "leftLabel" -> {
                        out.leftLabel = input.nextString()
                    }
                    "rightLabel" -> {
                        out.rightLabel = input.nextString()
                    }
                    "midLabel" -> {
                        out.middleLabel = input.nextString()
                    }
                    "fractional" -> {
                        out.isFractional = input.nextBoolean()
                    }
                    else -> {
                        input.skipValue()
                    }
                }
            }
            input.endObject()

            return out
        }

        override fun write(out: JsonWriter, value: RatingOptions) {
            out.beginObject()
            out.name("type").value(value.type.ordinal)
            out.name("stars").value(value.stars)
            out.name("left").value(value.leftMost)
            out.name("right").value(value.rightMost)
            out.name("leftLabel").value(value.leftLabel)
            out.name("midLabel").value(value.middleLabel)
            out.name("rightLabel").value(value.rightLabel)
            out.name("fractional").value(value.isFractional)
            out.endObject()
        }
    }


    enum class DisplayType(val nameResourceId: Int) {
        Star(R.string.property_rating_display_type_stars),
        Likert(R.string.property_rating_display_type_likert)
    }

    var type: DisplayType = DisplayType.Star

    var stars: Int = 5

    var leftMost: Int = 1
    var rightMost: Int = 5
    var leftLabel: String = ""
    var middleLabel: String = ""
    var rightLabel: String = ""
    var isFractional: Boolean = true


    constructor()

    constructor(context: Context) {
        leftLabel = context.resources.getString(R.string.property_rating_options_leftmost_label_example)
        rightLabel = context.resources.getString(R.string.property_rating_options_rightmost_label_example)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other is RatingOptions) {
            return other.isFractional == this.isFractional &&
                    other.type == this.type &&
                    other.leftMost == this.leftMost &&
                    other.leftLabel == this.leftLabel &&
                    other.middleLabel == this.middleLabel &&
                    other.rightMost == this.rightMost &&
                    other.rightLabel == this.rightLabel &&
                    other.stars == this.stars
        } else return false
    }

    override fun toString(): String {
        return "{RationOptions | isFractional: $isFractional , type: $type, stars: $stars, leftMost: $leftMost, rightMost: $rightMost, leftLabel: $leftLabel, rightLabel: $rightLabel, middleLable: $middleLabel"
    }

    fun getMaximumPrecisionIntegerRangeLength(): Short {
        return when (type) {
            DisplayType.Star -> {
                if (isFractional) {
                    stars * 2
                } else {
                    stars
                }
            }

            DisplayType.Likert -> {
                if (isFractional) {
                    (rightMost - leftMost) * 10
                } else {
                    rightMost - leftMost
                }
            }
        }.toShort()
    }

    fun convertFractionToRealScore(fraction: Fraction): Float {
        val under = getMaximumPrecisionIntegerRangeLength()
        val score = when (type) {
            DisplayType.Star -> {
                var upper = Math.round((fraction.toFloat() * under))
                if (fraction.upper > 0) {
                    upper = Math.max(upper, 1)
                }

                (upper.toFloat() / under) * stars
            }

            DisplayType.Likert -> {
                var upper = (fraction.toFloat() * under).nearestInt()
                if (fraction.upper > 0) {
                    upper = Math.max(upper, 1)
                }

                (upper.toFloat() / under) * (rightMost - leftMost) + leftMost
            }
        }

        return (score * 10).nearestInt() / 10f
    }
}