package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.conditions.time

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.trigger_display_time_ema.view.*
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.views.time.HourRangePicker
import java.util.*

class SamplingTimeConditionDisplayView : ConstraintLayout {

    var samplingCount: Short
        get() {
            return try {
                ui_text_sampling_count.text.toString().toShort()
            } finally {
                -1
            }
        }
        set(value) {
            ui_text_sampling_count.text = value.toString()
        }

    var nextAlertTime: Long? = null
        set(value) {
            field = value
            if (value == null) {
                ui_text_next_time_info.visibility = GONE
            } else {
                ui_text_next_time_info.visibility = View.VISIBLE
                ui_text_next_time_info.text = String.format(resources.getString(R.string.msg_trigger_ema_display_next_alert_format), (context.applicationContext as OTAndroidApp).applicationComponent.getLocalTimeFormats().FORMAT_DAY_WITHOUT_YEAR.format(Date(value)))
            }
        }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_display_time_ema, this, true)
    }

    fun setSamplingRange(from: Byte, to: Byte) {
        if (from == to || (from == 0.toByte() && to == 24.toByte())) {
            ui_text_hour_range.setText(R.string.msg_full_day)
        } else {
            ui_text_hour_range.text = "${HourRangePicker.getTimeText(context, from.toInt(), false)} -\n${HourRangePicker.getTimeText(context, to.toInt(), to < from)}"
        }
    }

    fun setSamplingFullDay() {
        ui_text_hour_range.setText(R.string.msg_full_day)
    }
}