package kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.NumberStyle

/**
 * Created by Young-Ho Kim on 2016-09-05.
 */
class NumberStylePropertyView(context: Context, attrs: AttributeSet?) : APropertyView<NumberStyle>(R.layout.component_property_number_style, context, attrs) {

    override var value: NumberStyle
        get() {
            return numberStyle
        }
        set(value) {
            if (numberStyle != value) {
                applyStyleToView(value)
                onValueChanged(value)
            }
        }

    private var numberStyle: NumberStyle

    private val unitPositionSelectionView: SelectionPropertyView
    private val unitTextView: ShortTextPropertyView
    private val pluralizeCheckView: BooleanPropertyView
    private val showCommasCheckView: BooleanPropertyView
    private val showFractionView: BooleanPropertyView
    private val fractionalDigitCountView: NumericUpDownPropertyView

    private var suspendEvent: Boolean = false

    private val onBooleanValueChangedHandler = {
        sender: Any, value: Boolean ->
        onBooleanValueChanged(sender, value)
    }

    init {

        layoutTransition = LayoutTransition()

        unitTextView = findViewById(R.id.ui_unit_text) as ShortTextPropertyView
        unitTextView.valueChanged += {
            sender, text ->
            updateCache(!suspendEvent)
        }


        pluralizeCheckView = findViewById(R.id.ui_pluralize) as BooleanPropertyView

        unitPositionSelectionView = findViewById(R.id.ui_unit_position) as SelectionPropertyView
        unitPositionSelectionView.setEntries(NumberStyle.UnitPosition.values().map { context.getString(it.nameResId) }.toTypedArray())
        unitPositionSelectionView.valueChanged += {
            sender, index ->
            if (index == NumberStyle.UnitPosition.None.ordinal) {
                unitTextView.visibility = GONE
                pluralizeCheckView.visibility = GONE
            } else {
                unitTextView.visibility = VISIBLE
                pluralizeCheckView.visibility = VISIBLE
            }

            updateCache(!suspendEvent)
        }

        showCommasCheckView = findViewById(R.id.ui_show_commas) as BooleanPropertyView
        showFractionView = findViewById(R.id.ui_show_fraction) as BooleanPropertyView

        fractionalDigitCountView = findViewById(R.id.ui_fractional_count) as NumericUpDownPropertyView
        fractionalDigitCountView.valueChanged += {
            sender, text ->
            updateCache(!suspendEvent)
        }
        fractionalDigitCountView.picker.minValue = 1
        fractionalDigitCountView.picker.maxValue = 5

        showFractionView.valueChanged += onBooleanValueChangedHandler
        pluralizeCheckView.valueChanged += onBooleanValueChangedHandler
        showCommasCheckView.valueChanged += onBooleanValueChangedHandler

        numberStyle = extractStyleFromView()
    }

    fun applyStyleToView(style: NumberStyle) {

        suspendEvent = true

        unitPositionSelectionView.value = style.unitPosition.ordinal
        unitTextView.value = style.unit
        pluralizeCheckView.value = style.pluralizeUnit
        showCommasCheckView.value = style.commaUnit != 0
        showFractionView.value = style.fractionPart != 0

        if (showFractionView.value) {
            fractionalDigitCountView.visibility = VISIBLE
        } else {
            fractionalDigitCountView.visibility = GONE
        }

        fractionalDigitCountView.value = style.fractionPart

        suspendEvent = false
    }

    fun extractStyleFromView(): NumberStyle {
        val newStyle = NumberStyle()
        newStyle.unitPosition = NumberStyle.UnitPosition.values()[unitPositionSelectionView.value]
        newStyle.commaUnit = if (showCommasCheckView.value) 3 else 0
        newStyle.fractionPart = if (showFractionView.value) fractionalDigitCountView.value else 0
        newStyle.unit = if (unitPositionSelectionView.value != NumberStyle.UnitPosition.None.ordinal) unitTextView.value else ""
        newStyle.pluralizeUnit = pluralizeCheckView.value
        return newStyle
    }

    private fun onBooleanValueChanged(sender: Any, value: Boolean) {
        if (sender === showFractionView) {
            if (value) {
                fractionalDigitCountView.visibility = VISIBLE
            } else {
                fractionalDigitCountView.visibility = GONE
            }
        }

        updateCache(!suspendEvent)
    }

    private fun updateCache(dispatchEvent: Boolean = true) {
        val extracted = extractStyleFromView()

        if (numberStyle != extracted) {
            numberStyle = extracted
            if (dispatchEvent)
                onValueChanged(extracted)
        }
    }

    override fun focus() {

    }


    override fun getSerializedValue(): String? {
        return NumberStyle.parser.toJson(value)
    }

    override fun setSerializedValue(serialized: String): Boolean {
        try {
            value = NumberStyle.parser.fromJson(serialized, NumberStyle::class.java)
            return true
        } catch(e: Exception) {
            try {
                value = Gson().fromJson(serialized, NumberStyle::class.java)
                return true
            } catch(e: Exception) {
                return false
            }
        }
    }

}