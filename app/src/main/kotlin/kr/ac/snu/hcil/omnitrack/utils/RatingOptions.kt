package kr.ac.snu.hcil.omnitrack.utils

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class RatingOptions {
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
}