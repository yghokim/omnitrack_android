package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.data

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.connection.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.utils.dipSize
import kr.ac.snu.hcil.omnitrack.utils.setPaddingTop
import org.jetbrains.anko.dip
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Created by Young-Ho on 9/5/2016.
 */
class DataDrivenTriggerDisplayView : LinearLayout {

    private val measureNameView: TextView by bindView(R.id.ui_measure_name)
    private val symbolView: AppCompatImageView by bindView(R.id.ui_comparison_symbol)
    private val comparedNumberView: TextView by bindView(R.id.ui_compared_number)

    val numberFormat = DecimalFormat("#,###.###")

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_display_data_driven, this, true)
    }

    fun setMeasureFactory(factory: OTMeasureFactory?) {
        measureNameView.visibility = View.VISIBLE
        measureNameView.text = factory?.getFormattedName()
    }

    fun setThreshold(threshold: BigDecimal?) {
        if (threshold != null) {
            comparedNumberView.setPaddingTop(0)
            comparedNumberView.text = numberFormat.format(threshold)
            comparedNumberView.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.number_digit_size))
        } else {
            setNullCondition()
        }
    }

    fun setComparison(method: OTDataDrivenTriggerCondition.ComparisonMethod?) {
        if (method != null) {
            symbolView.visibility = View.VISIBLE
            symbolView.setImageResource(method.symbolImageResourceId)
        } else {
            setNullCondition()
        }
    }

    fun setNullCondition() {
        symbolView.visibility = GONE
        measureNameView.visibility = GONE


        comparedNumberView.setText(R.string.msg_trigger_event_msg_tap_to_configure)

        comparedNumberView.setPaddingTop(dip(10f))
        comparedNumberView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dipSize(context, 20f))
    }
}