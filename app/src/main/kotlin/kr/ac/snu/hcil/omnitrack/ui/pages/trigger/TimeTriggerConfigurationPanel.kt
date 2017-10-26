package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.DatePickerDialog
import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.bindView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTimeTrigger
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DateTimePicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DayOfWeekSelector
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.HourRangePicker
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.BooleanPropertyView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.properties.ComboBoxPropertyView
import kr.ac.snu.hcil.omnitrack.utils.*
import kr.ac.snu.hcil.omnitrack.utils.events.IEventListener
import kr.ac.snu.hcil.omnitrack.utils.time.Time
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Young-Ho Kim on 2016-08-24
 */
class TimeTriggerConfigurationPanel : LinearLayout, IConditionConfigurationView, IEventListener<Int>, CompoundButton.OnCheckedChangeListener, DatePickerDialog.OnDateSetListener, View.OnClickListener {
    private val dateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_ymd))

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

    private var currentCondition: OTTimeTriggerCondition? = null

    private var suspendConditionChangeEvent = false

    private val conditionChanged = PublishSubject.create<ATriggerCondition>()
    override val onConditionChanged: Observable<ATriggerCondition>
        get() = conditionChanged

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        //TODO change to merge
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        addView(inflater.inflate(R.layout.trigger_time_trigger_config_panel, this, false))
        orientation = LinearLayout.VERTICAL

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

        timePicker.timeChanged += { sender, time ->
            currentCondition?.alarmTimeHour = ((timePicker.hour + timePicker.amPm * 12) % 24).toByte()
            currentCondition?.alarmTimeMinute = timePicker.minute.toByte()
            notifyConditionChanged()
        }


        dayOfWeekPicker.allowNoneSelection = false

        dayOfWeekPicker.selectionFlagsChanged += { sender, flags ->
            currentCondition?.dayOfWeekFlags = flags.toByte()
            notifyConditionChanged()
        }

        timeSpanCheckBox.setOnCheckedChangeListener(this)

        isRepeatedView.valueChanged += {
            sender, value ->
            applyIsRepeated(value, true)
            currentCondition?.isRepeated = value
            notifyConditionChanged()
        }

        durationPicker.onSecondsChanged += { sender, seconds ->
            if (currentCondition?.intervalSeconds != seconds.toShort()) {
                currentCondition?.intervalSeconds = seconds.toShort()
                notifyConditionChanged()
            }
        }

        timeSpanPicker.onRangeChanged += { sender, range ->
            if (currentCondition?.intervalHourRangeStart != range.first.toByte()
                    || currentCondition?.intervalHourRangeEnd != range.second.toByte()) {
                currentCondition?.intervalHourRangeStart = range.first.toByte()
                currentCondition?.intervalHourRangeEnd = range.second.toByte()
                notifyConditionChanged()
            }
        }

        InterfaceHelper.removeButtonTextDecoration(endDateButton)
        endDateButton.setOnClickListener(this)
        isEndSpecifiedCheckBox.setOnCheckedChangeListener(this)
    }

    private fun applyIsRepeated(isRepeated: Boolean, animate: Boolean) {
        if (animate) TransitionManager.beginDelayedTransition(this)
        if (isRepeated) {
            repetitionConfigGroup.visibility = View.VISIBLE
            timeSpanCheckBox.isChecked = true
            timeSpanCheckBox.isEnabled = true
        } else {
            repetitionConfigGroup.visibility = View.GONE
            timeSpanCheckBox.isChecked = false
            timeSpanCheckBox.isEnabled = false
        }
    }


    private fun applyConfigMode(mode: Byte, animate: Boolean) {
        refreshingViews = true
        if (animate) {
            TransitionManager.beginDelayedTransition(this)
        }

        when (mode) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {
                configTypePropertyView.value = 0
                alarmConfigGroup.visibility = VISIBLE
                intervalConfigGroup.visibility = GONE
            }

            OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                configTypePropertyView.value = 1
                alarmConfigGroup.visibility = GONE
                intervalConfigGroup.visibility = VISIBLE
            }
        }
        refreshingViews = false
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
                val conditionType = when (args) {
                    0 -> OTTimeTriggerCondition.TIME_CONDITION_ALARM
                    1 -> OTTimeTriggerCondition.TIME_CONDITION_INTERVAL
                    else -> OTTimeTriggerCondition.TIME_CONDITION_ALARM
                }

                applyConfigMode(conditionType, true)

                if (currentCondition?.timeConditionType != conditionType) {
                    currentCondition?.timeConditionType = conditionType
                    notifyConditionChanged()
                }
            }
        }
    }

    override fun onDateSet(p0: DatePicker?, year: Int, month: Int, day: Int) {
        applyRepeatEndDate(year, month, day)
        notifyConditionChanged()
    }


    private fun applyRepeatEndDate(year: Int, zeroBasedMonth: Int, day: Int) {
        repeatEndDate.set(Calendar.YEAR, year)
        repeatEndDate.set(Calendar.MONTH, zeroBasedMonth)
        repeatEndDate.set(Calendar.DAY_OF_MONTH, day)

        endDateButton.text = dateFormat.format(repeatEndDate.time)

        if (currentCondition?.endAt != repeatEndDate.timeInMillis) {
            currentCondition?.endAt = repeatEndDate.timeInMillis
            notifyConditionChanged()
        }
    }

    private fun applyRepeatEndDateToDayOffset(days: Int) {
        repeatEndDate.timeInMillis = System.currentTimeMillis() + days * 24 * 3600 * 1000L

        endDateButton.text = dateFormat.format(repeatEndDate.time)
    }

    override fun onCheckedChanged(view: CompoundButton, p1: Boolean) {
        if (view === timeSpanCheckBox) {
            TransitionManager.beginDelayedTransition(this)
            if (currentCondition?.intervalIsHourRangeUsed != timeSpanCheckBox.isChecked) {
                currentCondition?.intervalIsHourRangeUsed = timeSpanCheckBox.isChecked
                notifyConditionChanged()
            }

            if (timeSpanCheckBox.isChecked) {
                timeSpanPicker.visibility = View.VISIBLE
            } else {
                timeSpanPicker.visibility = View.GONE
            }

        } else if (view === isEndSpecifiedCheckBox) {
            TransitionManager.beginDelayedTransition(this)
            if (isEndSpecifiedCheckBox.isChecked) {
                endDateButton.visibility = View.VISIBLE

                if (currentCondition?.endAt != repeatEndDate.timeInMillis) {
                    currentCondition?.endAt = repeatEndDate.timeInMillis
                    notifyConditionChanged()
                }

            } else {
                endDateButton.visibility = View.INVISIBLE

                if (currentCondition?.endAt != null) {
                    currentCondition?.endAt = null
                    notifyConditionChanged()
                }
            }


        }


    }


    override fun applyCondition(condition: ATriggerCondition) {
        if (condition is OTTimeTriggerCondition && currentCondition != condition) {
            currentCondition = condition.clone() as OTTimeTriggerCondition

            suspendConditionChangeEvent = true


            applyIsRepeated(condition.isRepeated, false)
            applyConfigMode(condition.timeConditionType, false)
            dayOfWeekPicker.checkedFlagsInteger = condition.dayOfWeekFlags.toInt()

            val endAt = condition.endAt
            if (endAt != null) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = endAt
                isEndSpecifiedCheckBox.isChecked = true
                applyRepeatEndDate(cal.getYear(), cal.getZeroBasedMonth(), cal.getDayOfMonth())
            } else {
                isEndSpecifiedCheckBox.isChecked = false
                applyRepeatEndDateToDayOffset(1)
            }

            val time = Time(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
            timePicker.setTime(time.hour, time.minute, time.amPm)

            durationPicker.durationSeconds = condition.intervalSeconds.toInt()

            timeSpanCheckBox.isChecked = condition.intervalIsHourRangeUsed

            if (condition.intervalHourRangeStart == condition.intervalHourRangeEnd) {
                timeSpanPicker.fromHourOfDay = 9
                timeSpanPicker.toHourOfDay = 22
            } else {
                timeSpanPicker.fromHourOfDay = condition.intervalHourRangeStart.toInt()
                timeSpanPicker.toHourOfDay = condition.intervalHourRangeEnd.toInt()
            }

            suspendConditionChangeEvent = false
        }
    }

    private fun notifyConditionChanged() {
        if (!suspendConditionChangeEvent)
            currentCondition?.let { this.conditionChanged.onNext(it) }
    }
}