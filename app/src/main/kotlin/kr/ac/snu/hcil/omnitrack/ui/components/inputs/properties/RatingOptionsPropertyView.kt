package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.RatingOptions

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class RatingOptionsPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<RatingOptions>(R.layout.component_property_rating_options, context, attrs) {
    override var value: RatingOptions
        get() {
            val result = RatingOptions()
            result.allowIntermediate = allowIntermediatePropertyView.value
            result.starLevels = RatingOptions.StarLevel.values()[starLevelSelectionView.value]
            result.type = RatingOptions.DisplayType.values()[displayTypeSelectionView.value]

            result.leftLabel = leftLabelPropertyView.value
            result.middleLabel = middleLabelPropertyView.value
            result.rightLabel = rightLabelPropertyView.value

            result.leftMost = leftmostValuePicker.value
            result.rightMost = rightmostValuePicker.value

            return result
        }
        set(value) {
            allowIntermediatePropertyView.value = value.allowIntermediate
            starLevelSelectionView.value = value.starLevels.ordinal
            displayTypeSelectionView.value = value.type.ordinal

            leftLabelPropertyView.value = value.leftLabel
            middleLabelPropertyView.value = value.middleLabel
            rightLabelPropertyView.value = value.rightLabel

            leftmostValuePicker.value = value.leftMost
            rightmostValuePicker.value = value.rightMost
        }

    override fun focus() {
    }

    private val displayTypeSelectionView: SelectionPropertyView
    private val starLevelSelectionView: SelectionPropertyView

    private val likertOptionsGroup: ViewGroup

    private val leftmostValuePicker: NumericUpDownPropertyView
    private val rightmostValuePicker: NumericUpDownPropertyView

    private val leftLabelPropertyView: ShortTextPropertyView
    private val middleLabelPropertyView: ShortTextPropertyView
    private val rightLabelPropertyView: ShortTextPropertyView

    private val allowIntermediatePropertyView: BooleanPropertyView

    init {
        layoutTransition = LayoutTransition()

        displayTypeSelectionView = findViewById(R.id.ui_display_type) as SelectionPropertyView
        displayTypeSelectionView.setEntries(RatingOptions.DisplayType.values().map { resources.getString(it.nameResourceId) }.toTypedArray())
        displayTypeSelectionView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        starLevelSelectionView = findViewById(R.id.ui_star_levels) as SelectionPropertyView
        starLevelSelectionView.setEntries(RatingOptions.StarLevel.values().map { it.maxScore.toString() }.toTypedArray())
        starLevelSelectionView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        likertOptionsGroup = findViewById(R.id.ui_group_options_likert) as ViewGroup

        allowIntermediatePropertyView = findViewById(R.id.ui_allow_intermediate) as BooleanPropertyView
        allowIntermediatePropertyView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        leftmostValuePicker = likertOptionsGroup.findViewById(R.id.ui_leftmost_value) as NumericUpDownPropertyView
        rightmostValuePicker = likertOptionsGroup.findViewById(R.id.ui_rightmost_value) as NumericUpDownPropertyView
        leftLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_left_label) as ShortTextPropertyView
        middleLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_middle_label) as ShortTextPropertyView
        rightLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_right_label) as ShortTextPropertyView

        leftmostValuePicker.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        rightmostValuePicker.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        leftLabelPropertyView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        middleLabelPropertyView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }

        rightLabelPropertyView.valueChanged += {
            sender, v ->
            onValueChanged(value)
        }


        displayTypeSelectionView.valueChanged += {
            sender, index ->
            when (index) {
                RatingOptions.DisplayType.Likert.ordinal -> {
                    likertOptionsGroup.visibility = View.VISIBLE
                    starLevelSelectionView.visibility = View.GONE
                }

                RatingOptions.DisplayType.Star.ordinal -> {
                    likertOptionsGroup.visibility = View.GONE
                    starLevelSelectionView.visibility = View.VISIBLE
                }
            }
            onValueChanged(value)
        }
    }

    override fun watchOriginalValue() {
        //super.watchOriginalValue()
        displayTypeSelectionView.watchOriginalValue()
        starLevelSelectionView.watchOriginalValue()
        leftmostValuePicker.watchOriginalValue()
        rightmostValuePicker.watchOriginalValue()

        leftLabelPropertyView.watchOriginalValue()
        middleLabelPropertyView.watchOriginalValue()
        rightLabelPropertyView.watchOriginalValue()

        allowIntermediatePropertyView.watchOriginalValue()

    }

    override fun stopWatchOriginalValue() {
        //super.stopWatchOriginalValue()
        displayTypeSelectionView.stopWatchOriginalValue()
        starLevelSelectionView.stopWatchOriginalValue()
        leftmostValuePicker.stopWatchOriginalValue()
        rightmostValuePicker.stopWatchOriginalValue()

        leftLabelPropertyView.stopWatchOriginalValue()
        middleLabelPropertyView.stopWatchOriginalValue()
        rightLabelPropertyView.stopWatchOriginalValue()

        allowIntermediatePropertyView.stopWatchOriginalValue()
    }

    override fun getSerializedValue(): String? {
        return RatingOptions.parser.toJson(value)
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = RatingOptions.parser.fromJson(serialized, RatingOptions::class.java)
            return true
        } catch(e: Exception) {
            try {
                value = Gson().fromJson(serialized, RatingOptions::class.java)
                return true
            } catch(e: Exception) {
                return false
            }
        }
    }
}