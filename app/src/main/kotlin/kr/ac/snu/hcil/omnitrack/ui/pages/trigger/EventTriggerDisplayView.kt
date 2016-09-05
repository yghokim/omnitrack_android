package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.v7.widget.AppCompatImageView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.calculation.SingleNumericComparison
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import java.text.DecimalFormat

/**
 * Created by Young-Ho on 9/5/2016.
 */
class EventTriggerDisplayView: LinearLayout {

    private val measureNameView: TextView
    private val symbolView: AppCompatImageView
    private val comparedNumberView: TextView

    val numberFormat = DecimalFormat("#,###.###")

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init{
        orientation = VERTICAL

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_display_event, this, true)

        measureNameView = findViewById(R.id.ui_measure_name) as TextView
        symbolView = findViewById(R.id.ui_comparison_symbol) as AppCompatImageView
        comparedNumberView = findViewById(R.id.ui_compared_number) as TextView
    }

    fun setMeasureFactory(factory: OTMeasureFactory?)
    {
        measureNameView.text =if(factory != null) {
             factory.getFormattedName()
        }else "Select Measure"
    }

    fun setConditioner(conditioner: SingleNumericComparison?)
    {
        if(conditioner==null)
        {
            symbolView.visibility = GONE
            comparedNumberView.text = "Set Conditioner"
        }
        else{
            symbolView.visibility = View.VISIBLE

            symbolView.setImageResource(conditioner.method.symbolImageResourceId)
            comparedNumberView.text = numberFormat.format(conditioner.comparedTo)
        }

    }
}