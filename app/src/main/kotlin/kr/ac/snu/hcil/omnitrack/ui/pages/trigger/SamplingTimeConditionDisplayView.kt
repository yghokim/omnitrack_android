package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import kotlinx.android.synthetic.main.trigger_display_time_ema.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.HourRangePicker

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
            ui_text_hour_range.text = "${HourRangePicker.getTimeText(from.toInt(), false)} -\n${HourRangePicker.getTimeText(to.toInt(), to < from)}"
        }
    }
}