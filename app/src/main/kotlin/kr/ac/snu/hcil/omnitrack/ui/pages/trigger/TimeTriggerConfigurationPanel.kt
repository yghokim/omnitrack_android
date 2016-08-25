package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.DayOfWeekSelector
import kr.ac.snu.hcil.omnitrack.ui.components.common.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.HourRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.utils.IconNameEntryArrayAdapter

/**
 * Created by Young-Ho Kim on 2016-08-24.
 */
class TimeTriggerConfigurationPanel : LinearLayout, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    private val configSpinner: Spinner
    private val intervalConfigGroup: ViewGroup
    private val alarmConfigGroup: ViewGroup

    private val durationPicker: DurationPicker
    private val timePicker: DateTimePicker
    private val dayOfWeekPicker: DayOfWeekSelector

    private val timeSpanCheckBox: CheckBox
    private val timeSpanPicker: HourRangePicker

    private val isRepeatedView: BooleanPropertyView

    private val repetitionConfigGroup: ViewGroup

    private var refreshingViews = false

    var configMode: Int = OTTimeTrigger.CONFIG_TYPE_ALARM
        set(value) {
            if (field != value) {
                field = value
                applyConfigMode(value, true)
            }
        }

    var IsRepeated: Boolean get() = isRepeatedView.value
        set(value) {
            isRepeatedView.valueChanged.suspend = true
            isRepeatedView.value = value
            isRepeatedView.valueChanged.suspend = false
            applyIsRepeated(value, true)
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

        configSpinner.onItemSelectedListener = this

        intervalConfigGroup = findViewById(R.id.ui_interval_group) as ViewGroup
        alarmConfigGroup = findViewById(R.id.ui_alarm_group) as ViewGroup


        durationPicker = findViewById(R.id.ui_duration_picker) as DurationPicker
        timePicker = findViewById(R.id.ui_time_picker) as DateTimePicker
        timePicker.mode = DateTimePicker.MINUTE
        timePicker.isDayUsed = false

        dayOfWeekPicker = findViewById(R.id.ui_day_of_week_selector) as DayOfWeekSelector
        dayOfWeekPicker.allowNoneSelection = false

        timeSpanCheckBox = findViewById(R.id.ui_checkbox_range_use_timespan) as CheckBox
        timeSpanCheckBox.setOnCheckedChangeListener(this)

        timeSpanPicker = findViewById(R.id.ui_range_timespan_picker) as HourRangePicker

        repetitionConfigGroup = findViewById(R.id.ui_repetition_config_group) as ViewGroup

        isRepeatedView = findViewById(R.id.ui_is_repeated) as BooleanPropertyView

        isRepeatedView.valueChanged += {
            sender, value ->
            applyIsRepeated(value, true)
        }

        applyConfigMode(OTTimeTrigger.CONFIG_TYPE_ALARM, false)
        applyIsRepeated(IsRepeated, false)
    }

    private fun applyIsRepeated(isRepeated: Boolean, animate: Boolean) {
        println("isRepeatedValue changed")
        TransitionManager.beginDelayedTransition(this)
        if (isRepeated)
            repetitionConfigGroup.visibility = View.VISIBLE
        else
            repetitionConfigGroup.visibility = View.GONE
    }


    private fun applyConfigMode(mode: Int, animate: Boolean) {
        refreshingViews = true
        if (animate) {
            TransitionManager.beginDelayedTransition(this)
        }

        when (mode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM -> {
                configSpinner.setSelection(0)
                alarmConfigGroup.visibility = VISIBLE
                intervalConfigGroup.visibility = GONE
                timeSpanCheckBox.visibility = GONE
                timeSpanPicker.visibility = GONE
            }

            OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                configSpinner.setSelection(1)
                alarmConfigGroup.visibility = GONE
                intervalConfigGroup.visibility = VISIBLE
                timeSpanCheckBox.visibility = VISIBLE
            }
        }
        refreshingViews = false
    }

    fun applyConfigVariables(variables: Int) {

        if (OTTimeTrigger.IntervalConfig.isSpecified(variables)) {
            when (configMode) {
                OTTimeTrigger.CONFIG_TYPE_ALARM -> {
                    timePicker.setTime(OTTimeTrigger.AlarmConfig.getHour(variables), OTTimeTrigger.AlarmConfig.getMinute(variables), OTTimeTrigger.AlarmConfig.getAmPm(variables))
                }

                OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                    durationPicker.durationSeconds = OTTimeTrigger.IntervalConfig.getIntervalSeconds(variables)
                }
            }
        }
    }

    fun applyRangeVariables(variables: Int) {
        if (OTTimeTrigger.Range.isAllDayUsed(variables)) {
            dayOfWeekPicker.checkedFlagsInteger = 0b1111111
        } else {
            dayOfWeekPicker.checkedFlagsInteger = OTTimeTrigger.Range.getAllDayOfWeekFlags(variables)
        }

        if (configMode == OTTimeTrigger.CONFIG_TYPE_INTERVAL) {
            if (OTTimeTrigger.Range.getStartHour(variables) == OTTimeTrigger.Range.getEndHour(variables)) // all day
            {
                timeSpanCheckBox.isChecked = false
                timeSpanPicker.fromHourOfDay = 9
                timeSpanPicker.toHourOfDay = 22
            } else {
                timeSpanCheckBox.isChecked = true
                timeSpanPicker.fromHourOfDay = OTTimeTrigger.Range.getStartHour(variables)
                timeSpanPicker.toHourOfDay = OTTimeTrigger.Range.getEndHour(variables)

            }
        }
    }

    fun extractConfigVariables(): Int {

        val result = when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM ->
                OTTimeTrigger.AlarmConfig.makeConfig(timePicker.hour, timePicker.minute, timePicker.amPm)
            OTTimeTrigger.CONFIG_TYPE_INTERVAL ->
                OTTimeTrigger.IntervalConfig.makeConfig(durationPicker.durationSeconds)
            else -> 0
        }
        return result
    }

    fun extractRangeVariables(): Int {
        return when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM ->
                OTTimeTrigger.Range.makeConfig(dayOfWeekPicker.checkedFlagsInteger)
            OTTimeTrigger.CONFIG_TYPE_INTERVAL ->
                if (timeSpanCheckBox.isChecked)
                    OTTimeTrigger.Range.makeConfig(dayOfWeekPicker.checkedFlagsInteger, timeSpanPicker.fromHourOfDay, timeSpanPicker.toHourOfDay)
                else OTTimeTrigger.Range.makeConfig(dayOfWeekPicker.checkedFlagsInteger)
            else -> 0
        }
    }


    override fun onCheckedChanged(view: CompoundButton, p1: Boolean) {
        if (view === timeSpanCheckBox) {
            TransitionManager.beginDelayedTransition(this)
            if (timeSpanCheckBox.isChecked) {
                timeSpanPicker.visibility = View.VISIBLE
            } else {
                timeSpanPicker.visibility = View.GONE
            }
        }
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