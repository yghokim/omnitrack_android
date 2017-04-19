package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DayOfWeekSelector
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.HourRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ComboBoxPropertyView
import kr.ac.snu.hcil.omnitrack.utils.*
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Young-Ho Kim on 2016-08-24
 */
class TimeTriggerConfigurationPanel : LinearLayout, ITriggerConfigurationCoordinator, IEventListener<Int>, CompoundButton.OnCheckedChangeListener, DatePickerDialog.OnDateSetListener, View.OnClickListener {

    private val dateFormat: DateFormat

    private val configTypePropertyView: ComboBoxPropertyView by bindView(R.id.ui_time_trigger_type)
    private val intervalConfigGroup: ViewGroup by bindView(R.id.ui_interval_group)
    private val alarmConfigGroup: ViewGroup by bindView(R.id.ui_alarm_group)
    private val durationPicker: DurationPicker by bindView(R.id.ui_duration_picker)
    private val timePicker: DateTimePicker by bindView(R.id.ui_time_picker)
    private val dayOfWeekPicker: DayOfWeekSelector by bindView(R.id.ui_day_of_week_selector)

    private val timeSpanCheckBox: CheckBox by bindView(R.id.ui_checkbox_interval_use_timespan)
    private val timeSpanPicker: HourRangePicker by bindView(R.id.ui_trigger_interval_timespan_picker)

    private val isRepeatedView: BooleanPropertyView by bindView(R.id.ui_is_repeated)

    private val isEndSpecifiedCheckBox: CheckBox by bindView(R.id.ui_checkbox_range_specify_end)
    private val endDateButton: Button by bindView(R.id.ui_button_end_date)

    private val repetitionConfigGroup: ViewGroup by bindView(R.id.ui_repetition_config_group)

    private val repeatEndDate: Calendar

    private var refreshingViews = false

    var configMode: Int = OTTimeTrigger.CONFIG_TYPE_ALARM
        set(value) {
            if (field != value) {
                field = value
                applyConfigMode(value, true)
            }
        }

    var isRepeated: Boolean get() = isRepeatedView.value
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
        //TODO change to merge
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.trigger_time_trigger_config_panel, this, false))
        orientation = LinearLayout.VERTICAL

        dateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_ymd))
        repeatEndDate = GregorianCalendar(2016, 1, 1)

        configTypePropertyView.adapter = IconNameEntryArrayAdapter(context,
                arrayOf(
                        IconNameEntryArrayAdapter.Entry(OTTimeTrigger.configIconId(OTTimeTrigger.CONFIG_TYPE_ALARM),
                                OTTimeTrigger.configNameId(OTTimeTrigger.CONFIG_TYPE_ALARM)),
                        IconNameEntryArrayAdapter.Entry(OTTimeTrigger.configIconId(OTTimeTrigger.CONFIG_TYPE_INTERVAL),
                                OTTimeTrigger.configNameId(OTTimeTrigger.CONFIG_TYPE_INTERVAL))
                ))

        configTypePropertyView.valueChanged += this

        timePicker.mode = DateTimePicker.MINUTE
        timePicker.isDayUsed = false

        timePicker.setTime(9, 0, Calendar.PM)


        dayOfWeekPicker.allowNoneSelection = false

        timeSpanCheckBox.setOnCheckedChangeListener(this)

        isRepeatedView.valueChanged += {
            sender, value ->
            applyIsRepeated(value, true)
        }

        InterfaceHelper.removeButtonTextDecoration(endDateButton)
        endDateButton.setOnClickListener(this)
        isEndSpecifiedCheckBox.setOnCheckedChangeListener(this)

        applyConfigMode(OTTimeTrigger.CONFIG_TYPE_ALARM, false)
        applyIsRepeated(isRepeated, false)
        applyRepeatEndDateToDayOffset(1)
    }

    private fun applyIsRepeated(isRepeated: Boolean, animate: Boolean) {
        if (animate) TransitionManager.beginDelayedTransition(this)
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
                configTypePropertyView.value = 0
                alarmConfigGroup.visibility = VISIBLE
                intervalConfigGroup.visibility = GONE
            }

            OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                configTypePropertyView.value = 1
                alarmConfigGroup.visibility = GONE
                intervalConfigGroup.visibility = VISIBLE
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

                    if (OTTimeTrigger.IntervalConfig.getStartHour(variables) == OTTimeTrigger.IntervalConfig.getEndHour(variables)) // all day
                    {
                        timeSpanCheckBox.isChecked = false
                        timeSpanPicker.fromHourOfDay = 9
                        timeSpanPicker.toHourOfDay = 22
                    } else {
                        timeSpanCheckBox.isChecked = true
                        timeSpanPicker.fromHourOfDay = OTTimeTrigger.IntervalConfig.getStartHour(variables)
                        timeSpanPicker.toHourOfDay = OTTimeTrigger.IntervalConfig.getEndHour(variables)

                    }

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

        if (OTTimeTrigger.Range.isEndSpecified(variables)) {
            isEndSpecifiedCheckBox.isChecked = true
            applyRepeatEndDate(OTTimeTrigger.Range.getEndYear(variables), OTTimeTrigger.Range.getEndZeroBasedMonth(variables), OTTimeTrigger.Range.getEndDay(variables))
        } else {
            isEndSpecifiedCheckBox.isChecked = false
        }
    }

    fun extractConfigVariables(): Int {

        val result = when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_ALARM ->
                OTTimeTrigger.AlarmConfig.makeConfig(timePicker.hour, timePicker.minute, timePicker.amPm)
            OTTimeTrigger.CONFIG_TYPE_INTERVAL ->
                if (timeSpanCheckBox.isChecked) {
                    OTTimeTrigger.IntervalConfig.makeConfig(durationPicker.durationSeconds, timeSpanPicker.fromHourOfDay, timeSpanPicker.toHourOfDay)
                } else {
                    OTTimeTrigger.IntervalConfig.makeConfig(durationPicker.durationSeconds)
                }
            else -> 0
        }
        return result
    }

    fun extractRangeVariables(): Int {

        return if (isRepeated) {

            if (isEndSpecifiedCheckBox.isChecked) {
                println("end specified.")
                OTTimeTrigger.Range.makeConfig(dayOfWeekPicker.checkedFlagsInteger, repeatEndDate.getYear(), repeatEndDate.getZeroBasedMonth(), repeatEndDate.getDayOfMonth())
            } else {
                println("end not specified")
                OTTimeTrigger.Range.makeConfig(dayOfWeekPicker.checkedFlagsInteger)
            }
        } else 0
    }

    override fun onClick(view: View) {
        if (view === endDateButton) {
            val dialog = DatePickerDialog(context, this, repeatEndDate.getYear(), repeatEndDate.getZeroBasedMonth(), repeatEndDate.getDayOfMonth())
            dialog.datePicker.minDate = System.currentTimeMillis()
            dialog.datePicker.maxDate = System.currentTimeMillis() + 10 * 365 * 24 * 3600 * 1000L
            dialog.show()
        }
    }

    override fun onEvent(sender: Any, args: Int) {
        println("onEvent: $sender")
        if (sender === configTypePropertyView) {
            if (!refreshingViews) {
                when (args) {
                    0 -> configMode = OTTimeTrigger.CONFIG_TYPE_ALARM
                    1 -> configMode = OTTimeTrigger.CONFIG_TYPE_INTERVAL
                }
            }
        }
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, day: Int) {
        applyRepeatEndDate(year, month, day)
    }


    private fun applyRepeatEndDate(year: Int, zeroBasedMonth: Int, day: Int) {
        repeatEndDate.set(Calendar.YEAR, year)
        repeatEndDate.set(Calendar.MONTH, zeroBasedMonth)
        repeatEndDate.set(Calendar.DAY_OF_MONTH, day)

        endDateButton.text = dateFormat.format(repeatEndDate.time)
    }

    private fun applyRepeatEndDateToDayOffset(days: Int) {
        repeatEndDate.timeInMillis = System.currentTimeMillis() + days * 24 * 3600 * 1000L

        endDateButton.text = dateFormat.format(repeatEndDate.time)
    }

    override fun onCheckedChanged(view: CompoundButton, p1: Boolean) {
        if (view === timeSpanCheckBox) {
            TransitionManager.beginDelayedTransition(this)
            if (timeSpanCheckBox.isChecked) {
                timeSpanPicker.visibility = View.VISIBLE
            } else {
                timeSpanPicker.visibility = View.GONE
            }
        } else if (view === isEndSpecifiedCheckBox) {
            TransitionManager.beginDelayedTransition(this)
            if (isEndSpecifiedCheckBox.isChecked) {
                endDateButton.visibility = View.VISIBLE
            } else {
                endDateButton.visibility = View.INVISIBLE
            }
        }
    }

    override fun applyConfigurationToTrigger(trigger: OTTrigger) {
        if (trigger is OTTimeTrigger) {
            trigger.configType = configMode
            trigger.isRepeated = isRepeated
            trigger.configVariables = extractConfigVariables()
            trigger.rangeVariables = extractRangeVariables()
        }
    }

    override fun importTriggerConfiguration(trigger: OTTrigger) {
        if (trigger is OTTimeTrigger) {
            configMode = trigger.configType
            isRepeated = trigger.isRepeated
            applyConfigVariables(trigger.configVariables)
            applyRangeVariables(trigger.rangeVariables)
        }
    }

    override fun validateConfigurations(errorMessagesOut: MutableList<String>): Boolean {
        var validated = true
        when (configMode) {
            OTTimeTrigger.CONFIG_TYPE_INTERVAL -> {
                if (durationPicker.durationSeconds == 0) {
                    errorMessagesOut.add(resources.getString(R.string.msg_trigger_error_interval_not_0))
                    validated = false
                }
            }

            OTTimeTrigger.CONFIG_TYPE_ALARM -> {

            }
        }

        return validated
    }

    override fun writeConfigurationToBundle(out: Bundle) {
        out.putInt("configMode", configMode)
        out.putBoolean("isRepeated", isRepeated)
        out.putInt("configVariables", extractConfigVariables())
        out.putInt("rangeVariables", extractRangeVariables())
    }

    override fun readConfigurationFromBundle(bundle: Bundle) {
        configMode = bundle.getInt("configMode")
        isRepeated = bundle.getBoolean("isRepeated")
        applyConfigVariables(bundle.getInt("configVariables"))
        applyRangeVariables(bundle.getInt("rangeVariables"))
    }

}