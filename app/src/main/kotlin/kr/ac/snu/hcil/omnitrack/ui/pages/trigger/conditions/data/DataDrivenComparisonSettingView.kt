package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.components.common.ExtendedSpinner
import kr.ac.snu.hcil.omnitrack.utils.EnterHideKeyboardEditorActionListener
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.inflateContent
import java.math.BigDecimal
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 9. 5..
 */
class DataDrivenComparisonSettingView : LinearLayout, TextWatcher, ExtendedSpinner.OnItemSelectedListener {

    var threshold: BigDecimal = BigDecimal.ZERO
        set (value) {
            if (field != value) {
                field = value

                if (!numberField.text.toString().toBigDecimal().equals(value))
                    numberField.setText(value.toPlainString(), TextView.BufferType.EDITABLE)

                onThresholdChanged.invoke(this, value)
            }
        }

    var comparison: OTDataDrivenTriggerCondition.ComparisonMethod by Delegates.observable(OTDataDrivenTriggerCondition.ComparisonMethod.Exceed) { prop, old, new ->
        if (old != new) {
            spinner.selectedItemPosition = OTDataDrivenTriggerCondition.ComparisonMethod.values().indexOf(new)
            onComparisonChanged.invoke(this, new)
        }
    }

    val onThresholdChanged = Event<BigDecimal>()
    val onComparisonChanged = Event<OTDataDrivenTriggerCondition.ComparisonMethod>()

    private val spinner: ExtendedSpinner
    private val numberField: EditText

    private val adapter = ComparisonMethodAdapter()


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


    init {
        orientation = LinearLayout.HORIZONTAL
        inflateContent(R.layout.conditioner_setting_single_numeric_comparison, true)

        spinner = findViewById(R.id.ui_comparison_method)
        spinner.adapter = adapter

        numberField = findViewById(R.id.ui_compared_number_input)

        //synchronize
        numberField.setText(threshold.toPlainString(), TextView.BufferType.EDITABLE)
        spinner.selectedItemPosition = OTDataDrivenTriggerCondition.ComparisonMethod.values().indexOf(comparison)

        //add event listeners
        spinner.onItemSelectedListener = this
        numberField.addTextChangedListener(this)
        numberField.setOnEditorActionListener(EnterHideKeyboardEditorActionListener())

    }

    override fun onItemSelected(spinner: ExtendedSpinner, position: Int) {
        this.comparison = spinner.selectedItem as OTDataDrivenTriggerCondition.ComparisonMethod
    }

    override fun afterTextChanged(s: Editable) {
        val decimal = s.toString().toBigDecimalOrNull()
        if (decimal != null)
            this.threshold = decimal
        else {
            this.numberField.setText(threshold.toPlainString())
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    inner class ComparisonMethodAdapter : ArrayAdapter<OTDataDrivenTriggerCondition.ComparisonMethod>(context, R.layout.simple_list_element_single_big_icon,
            OTDataDrivenTriggerCondition.ComparisonMethod.values()) {


        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = getView(position, convertView, parent)
            view.setBackgroundResource(R.drawable.bottom_separator_thin)

            return view
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val view = convertView ?:
                    LayoutInflater.from(parent.context).inflate(R.layout.simple_list_element_single_big_icon, parent, false)

            if (view.tag !is MethodViewHolder) {
                view.tag = MethodViewHolder(view)
            }

            val holder = view.tag as MethodViewHolder
            holder.bind(getItem(position))
            return view
        }

        inner class MethodViewHolder(val view: View) {

            private val iconView: AppCompatImageView = view.findViewById(R.id.textView)

            fun bind(method: OTDataDrivenTriggerCondition.ComparisonMethod) {
                iconView.setImageResource(method.symbolImageResourceId)
            }
        }
    }
}