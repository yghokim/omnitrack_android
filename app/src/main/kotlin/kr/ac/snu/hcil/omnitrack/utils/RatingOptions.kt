package kr.ac.snu.hcil.omnitrack.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class RatingOptions {

    companion object {
        val parser: Gson by lazy {
            GsonBuilder().registerTypeAdapter(RatingOptions::class.java, RatingOptionsTypeAdapter()).create()
        }
    }

    internal class RatingOptionsTypeAdapter : TypeAdapter<RatingOptions>() {
        override fun read(input: JsonReader): RatingOptions {

            return RatingOptions().apply {

                input.beginObject()
                input.nextName()
                this.type = DisplayType.values()[input.nextInt()]

                input.nextName()
                this.starLevels = StarLevel.values()[input.nextInt()]

                input.nextName()
                this.leftMost = input.nextInt()

                input.nextName()
                this.rightMost = input.nextInt()

                input.nextName()
                this.leftLabel = input.nextString()

                input.nextName()
                this.middleLabel = input.nextString()

                input.nextName()
                this.rightLabel = input.nextString()

                input.nextName()
                this.allowIntermediate = input.nextBoolean()

                input.endObject()
            }
        }

        override fun write(out: JsonWriter, value: RatingOptions) {
            out.beginObject()
            out.name("t").value(value.type.ordinal)
            out.name("s").value(value.starLevels.ordinal)
            out.name("lv").value(value.leftMost)
            out.name("rv").value(value.rightMost)
            out.name("ll").value(value.leftLabel)
            out.name("ml").value(value.middleLabel)
            out.name("rl").value(value.rightLabel)
            out.name("ai").value(value.allowIntermediate)
            out.endObject()
        }
    }


    enum class DisplayType(val nameResourceId: Int) {
        Star(R.string.property_rating_display_type_stars),
        Likert(R.string.property_rating_display_type_likert)
    }

    enum class StarLevel(val maxScore: Int) {
        Level5(5), Level7(7), Level10(10)
    }

    var type: DisplayType = DisplayType.Star

    var starLevels: StarLevel = StarLevel.Level5

    var leftMost: Int = 1
    var rightMost: Int = 5
    var leftLabel: String = OTApplication.app.resources.getString(R.string.property_rating_options_leftmost_label_example)
    var middleLabel: String = ""
    var rightLabel: String = OTApplication.app.resources.getString(R.string.property_rating_options_rightmost_label_example)
    var allowIntermediate: Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (other is RatingOptions) {
            return other.allowIntermediate == this.allowIntermediate &&
                    other.type == this.type &&
                    other.leftMost == this.leftMost &&
                    other.leftLabel == this.leftLabel &&
                    other.middleLabel == this.middleLabel &&
                    other.rightMost == this.rightMost &&
                    other.rightLabel == this.rightLabel &&
                    other.starLevels == this.starLevels
        } else return false
    }
}