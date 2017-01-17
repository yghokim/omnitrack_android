package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditionerviews

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.calculation.SingleNumericComparison
import kr.ac.snu.hcil.omnitrack.ui.components.common.ExtendedSpinner
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 16. 9. 5..
 */
class SingleNumericConditionerSettingView : LinearLayout {

    var conditioner: SingleNumericComparison
        get() {
            val result = SingleNumericComparison()
            result.method = spinner.selectedItem as SingleNumericComparison.ComparisonMethod
            result.comparedTo = numberField.text.toString().toDouble()
            return result
        }
        set(value) {
            println("set method: ${value.method}")
            spinner.selectedItemPosition = value.method.ordinal
            numberField.setText(value.comparedTo.toString(), TextView.BufferType.NORMAL)
        }

    private val spinner: ExtendedSpinner
    private val numberField: EditText

    private val adapter = ComparisonMethodAdapter()


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)


    init {
        orientation = LinearLayout.HORIZONTAL
        inflateContent(R.layout.conditioner_setting_single_numeric_comparison, true)

        spinner = findViewById(R.id.ui_comparison_method) as ExtendedSpinner
        spinner.adapter = adapter


        numberField = findViewById(R.id.ui_compared_number_input) as EditText

    }

    inner class ComparisonMethodAdapter : ArrayAdapter<SingleNumericComparison.ComparisonMethod>(context, R.layout.simple_list_element_single_big_icon,
            SingleNumericComparison.ComparisonMethod.values()) {


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

            private val iconView: AppCompatImageView = view.findViewById(R.id.textView) as AppCompatImageView

            fun bind(method: SingleNumericComparison.ComparisonMethod) {
                iconView.setImageResource(method.symbolImageResourceId)
            }
        }
    }
}