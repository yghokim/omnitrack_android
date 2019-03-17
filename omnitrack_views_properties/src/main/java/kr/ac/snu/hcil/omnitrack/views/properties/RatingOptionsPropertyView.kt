package kr.ac.snu.hcil.omnitrack.views.properties

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import kr.ac.snu.hcil.android.common.events.IEventListener
import kr.ac.snu.hcil.omnitrack.core.types.RatingOptions

/**
 * Created by Young-Ho Kim on 2016-09-23.
 */
class RatingOptionsPropertyView(context: Context, attrs: AttributeSet?) : APropertyView<RatingOptions>(R.layout.component_property_rating_options, context, attrs) {

    private val selectableStarLevels = arrayOf(5, 7, 10)

    override var value: RatingOptions
        get() {
            val result = RatingOptions(context)
            result.isFractional = allowIntermediatePropertyView.value
            result.stars = selectableStarLevels[starLevelSelectionView.value]
            result.type = RatingOptions.DisplayType.values()[displayTypeSelectionView.value]

            result.leftLabel = leftLabelPropertyView.value
            result.middleLabel = middleLabelPropertyView.value
            result.rightLabel = rightLabelPropertyView.value

            result.leftMost = leftmostValuePicker.value
            result.rightMost = rightmostValuePicker.value

            return result
        }
        set(value) {
            allowIntermediatePropertyView.value = value.isFractional
            starLevelSelectionView.value = selectableStarLevels.indexOf(value.stars)
            displayTypeSelectionView.value = value.type.ordinal

            leftLabelPropertyView.value = value.leftLabel
            middleLabelPropertyView.value = value.middleLabel
            rightLabelPropertyView.value = value.rightLabel

            leftmostValuePicker.value = value.leftMost
            rightmostValuePicker.value = value.rightMost
        }

    override fun focus() {
    }

    private val displayTypeSelectionView: SelectionPropertyView = findViewById(R.id.ui_display_type)
    private val starLevelSelectionView: SelectionPropertyView = findViewById(R.id.ui_star_levels)

    private val likertOptionsGroup: ViewGroup = findViewById(R.id.ui_group_options_likert)

    private val leftmostValuePicker: NumericUpDownPropertyView
    private val rightmostValuePicker: NumericUpDownPropertyView

    private val leftLabelPropertyView: ShortTextPropertyView
    private val middleLabelPropertyView: ShortTextPropertyView
    private val rightLabelPropertyView: ShortTextPropertyView

    private val intListener = object : IEventListener<Int> {
        override fun onEvent(sender: Any, args: Int) {
            onValueChanged(value)
        }
    }

    private val stringListener = object : IEventListener<String> {
        override fun onEvent(sender: Any, args: String) {
            onValueChanged(value)
        }
    }

    private val booleanListener = object : IEventListener<Boolean> {
        override fun onEvent(sender: Any, args: Boolean) {
            onValueChanged(value)
        }

    }

    private val allowIntermediatePropertyView: BooleanPropertyView = findViewById(R.id.ui_allow_intermediate)

    init {
        layoutTransition = LayoutTransition()

        displayTypeSelectionView.setEntries(RatingOptions.DisplayType.values().map { resources.getString(it.nameResourceId) }.toTypedArray())
        displayTypeSelectionView.valueChanged += intListener

        starLevelSelectionView.setEntries(selectableStarLevels.map { it.toString() }.toTypedArray())
        starLevelSelectionView.valueChanged += intListener

        allowIntermediatePropertyView.valueChanged += booleanListener

        leftmostValuePicker = likertOptionsGroup.findViewById(R.id.ui_leftmost_value)
        rightmostValuePicker = likertOptionsGroup.findViewById(R.id.ui_rightmost_value)
        leftLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_left_label)
        middleLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_middle_label)
        rightLabelPropertyView = likertOptionsGroup.findViewById(R.id.ui_right_label)

        leftmostValuePicker.valueChanged += intListener

        rightmostValuePicker.valueChanged += intListener

        leftLabelPropertyView.valueChanged += stringListener

        middleLabelPropertyView.valueChanged += stringListener

        rightLabelPropertyView.valueChanged += stringListener


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

    override fun compareAndShowEdited(comparedTo: RatingOptions) {
        val orig = value
        displayTypeSelectionView.showEditedOnTitle = orig.type != comparedTo.type
        starLevelSelectionView.showEditedOnTitle = orig.stars != comparedTo.stars
        leftmostValuePicker.showEditedOnTitle = orig.leftMost != comparedTo.leftMost
        rightmostValuePicker.showEditedOnTitle = orig.rightMost != comparedTo.rightMost

        leftLabelPropertyView.showEditedOnTitle = orig.leftLabel != comparedTo.leftLabel
        middleLabelPropertyView.showEditedOnTitle = orig.middleLabel != comparedTo.middleLabel
        rightLabelPropertyView.showEditedOnTitle = orig.rightLabel != comparedTo.rightLabel

        allowIntermediatePropertyView.showEditedOnTitle = orig.isFractional != comparedTo.isFractional
    }

    override fun getSerializedValue(): String? {
        return RatingOptions.typeAdapter.toJson(value)
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = RatingOptions.typeAdapter.fromJson(serialized)
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