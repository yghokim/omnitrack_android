package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.LinearLayout
import android.widget.Spinner
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.SelectionView
import kr.ac.snu.hcil.omnitrack.utils.IconNameEntryArrayAdapter

/**
 * Created by Young-Ho Kim on 2016-08-24.
 */
class TimeTriggerConfigurationPanel : LinearLayout, AdapterView.OnItemSelectedListener {
    private val configSpinner: Spinner
    private val intervalConfigGroup: ViewGroup
    private val durationPicker: DurationPicker
    private val timePicker: DateTimePicker
    private val dayOfWeekPicker: SelectionView

    private var refreshingViews = false

    var configMode: Int = OTTimeTrigger.CONFIG_TYPE_ALARM
        set(value) {
            if (field != value) {
                field = value
                applyConfigMode(value, true)
            }
        }


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.component_time_trigger_config_panel, this, false))
        orientation = LinearLayout.VERTICAL

        configSpinner = findViewById(R.id.ui_spinner_config_type) as Spinner

        configSpinner.adapter = IconNameEntryArrayAdapter(context,
                arrayOf(
                        IconNameEntryArrayAdapter.Entry(OTTimeTrigger.configIconId(OTTimeTrigger.CONFIG_TYPE_ALARM),
                                OTTimeTrigger.configNameId(OTTimeTrigger.CONFIG_TYPE_ALARM)),
                        IconNameEntryArrayAdapter.Entry(OTTimeTrigger.configIconId(OTTimeTrigger.CONFIG_TYPE_INTERVAL),
                                OTTimeTrigger.configNameId(OTTimeTrigger.CONFIG_TYPE_INTERVAL))
                ))

        configSpinner.setSelection(0)

        configSpinner.setOnItemSelectedListener(this)

        intervalConfigGroup = findViewById(R.id.ui_interval_group) as ViewGroup

        durationPicker = findViewById(R.id.ui_duration_picker) as DurationPicker
        timePicker = findViewById(R.id.ui_time_picker) as DateTimePicker
        timePicker.mode = DateTimePicker.MINUTE
        timePicker.isDayUsed = false

        dayOfWeekPicker = findViewById(R.id.ui_day_of_week_selector) as SelectionView
        dayOfWeekPicker.setValues(resources.getStringArray(R.array.days_of_week_short))


        applyConfigMode(OTTimeTrigger.CONFIG_TYPE_ALARM, false)
    }


    private fun applyConfigMode(mode: Int, animate: Boolean) {
        refreshingViews = true
        if (animate) {
            TransitionManager.beginDelayedTransition(this)
        }

        when (mode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM -> {
                configSpinner.setSelection(0)
                timePicker.visibility = VISIBLE
                intervalConfigGroup.visibility = GONE
            }

            OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                configSpinner.setSelection(1)
                timePicker.visibility = GONE
                intervalConfigGroup.visibility = VISIBLE
            }
        }
        refreshingViews = false
    }

    fun applyConfigVariables(variables: Int) {
        when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM -> {
                timePicker.hour = OTTimeTrigger.AlarmConfig.getHour(variables)
                timePicker.minute = OTTimeTrigger.AlarmConfig.getMinute(variables)
            }

            OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                durationPicker.durationSeconds = OTTimeTrigger.IntervalConfig.getIntervalSeconds(variables)
            }
        }
    }

    fun applyRangeVariables(variables: Int) {


    }

    fun extractConfigVariables(): Int {

        return when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM ->
                OTTimeTrigger.AlarmConfig.makeConfig(timePicker.hour, timePicker.minute, timePicker.amPm)
            OTTimeTrigger.CONFIG_TYPE_INTERVAL ->
                OTTimeTrigger.IntervalConfig.makeConfig(durationPicker.durationSeconds)

            else -> 0
        }
    }

    fun extractRangeVariables(): Int {
        return 0
    }


    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (!refreshingViews) {
            when (position) {
                0 -> configMode = OTTimeTrigger.CONFIG_TYPE_ALARM
                1 -> configMode = OTTimeTrigger.CONFIG_TYPE_INTERVAL
            }
        }
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {

    }


}