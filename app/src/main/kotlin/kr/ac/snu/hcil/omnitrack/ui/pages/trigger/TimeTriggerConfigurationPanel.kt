package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.app.DatePickerDialog
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.transition.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.DatePicker
import butterknife.bindView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.trigger_time_trigger_config_panel.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerInformationHelper
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
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.jetbrains.anko.backgroundResource
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Young-Ho Kim on 2016-08-24
 */
class TimeTriggerConfigurationPanel : ConstraintLayout, IConditionConfigurationView, IEventListener<Int>, CompoundButton.OnCheckedChangeListener, DatePickerDialog.OnDateSetListener, View.OnClickListener {
    private val dateFormat = SimpleDateFormat(resources.getString(R.string.dateformat_ymd))

    private val configTypePropertyView: ComboBoxPropertyView by bindView(R.id.ui_time_trigger_type)
    private val intervalConfigGroup: View by bindView(R.id.ui_group_interval)
    private val alarmConfigGroup: View by bindView(R.id.ui_group_alarm)
    private val durationPicker: DurationPicker by bindView(R.id.ui_duration_picker)
    private val timePicker: DateTimePicker by bindView(R.id.ui_time_picker)
    private val dayOfWeekPicker: DayOfWeekSelector by bindView(R.id.ui_day_of_week_selector)

    private val timeSpanCheckBox: CheckBox by bindView(R.id.ui_checkbox_interval_use_timespan)
    private val timeSpanPicker: HourRangePicker by bindView(R.id.ui_trigger_interval_timespan_picker)

    private val isRepeatedView: BooleanPropertyView by bindView(R.id.ui_is_repeated)

    private val isEndSpecifiedCheckBox: CheckBox by bindView(R.id.ui_checkbox_range_specify_end)
    private val endDateButton: Button by bindView(R.id.ui_button_end_date)

    private val repetitionConfigGroup: View by bindView(R.id.ui_group_repitition_config)

    private val repeatEndDate: Calendar

    private var refreshingViews = false

    private var currentCondition: OTTimeTriggerCondition? = null

    private var suspendConditionChangeEvent = false


    private val conditionChanged = PublishSubject.create<ATriggerCondition>()
    override val onConditionChanged: Observable<ATriggerCondition>
        get() = conditionChanged

    private val modeChangeTransition: Transition by lazy {
        TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(Fade(Fade.MODE_OUT).setDuration(200))
                .addTransition(ChangeBounds().setDuration(400))
                .addTransition(Fade(Fade.MODE_IN).setDuration(500).setStartDelay(300))
    }

    private val toggleTransition: Transition by lazy {
        TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(Fade(Fade.MODE_OUT).setDuration(300))
                .addTransition(ChangeBounds().setDuration(250))
                .addTransition(Fade(Fade.MODE_IN).setDuration(300))
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        backgroundResource = R.drawable.bottom_separator_light

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.trigger_time_trigger_config_panel, this, true)

        repeatEndDate = GregorianCalendar(2016, 1, 1)

        configTypePropertyView.adapter = IconNameEntryArrayAdapter(context,
                arrayOf(OTTimeTriggerCondition.TIME_CONDITION_ALARM, OTTimeTriggerCondition.TIME_CONDITION_INTERVAL, OTTimeTriggerCondition.TIME_CONDITION_SAMPLING).map {
                    IconNameEntryArrayAdapter.Entry(OTTriggerInformationHelper.getTimeConfigIconResId(it)!!,
                            OTTriggerInformationHelper.getTimeConfigDescResId(it)!!)
                }.toTypedArray()
        )

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

        ui_ema_use_sampling_range.setOnCheckedChangeListener(this)
        ui_ema_range_picker.onRangeChanged += { sender, range ->
            if (currentCondition?.samplingHourStart != range.first.toByte()
                    || currentCondition?.samplingHourEnd != range.second.toByte()) {
                currentCondition?.samplingHourStart = range.first.toByte()
                currentCondition?.samplingHourEnd = range.second.toByte()
                notifyConditionChanged()
            }
        }
        ui_ema_count.valueChanged += { sender, count ->
            if (currentCondition?.samplingCount != count.toShort()) {
                currentCondition?.samplingCount = count.toShort()
                notifyConditionChanged()
            }
        }
        ui_ema_count.picker.minValue = 1
        ui_ema_count.picker.maxValue = 144

        ui_ema_minimum_interval_picker.max = (3 * TimeHelper.hoursInMilli / 1000).toInt()

        ui_ema_minimum_interval_picker.durationChanged += { sender, duration ->
            if (currentCondition?.samplingMinIntervalSeconds != duration.toShort()) {
                currentCondition?.samplingMinIntervalSeconds = duration.toShort()
                notifyConditionChanged()
            }
        }

        InterfaceHelper.removeButtonTextDecoration(endDateButton)
        endDateButton.setOnClickListener(this)
        isEndSpecifiedCheckBox.setOnCheckedChangeListener(this)
    }

    private fun applyIsRepeated(isRepeated: Boolean, animate: Boolean) {
        isRepeatedView.value = isRepeated
        if (animate) TransitionManager.beginDelayedTransition(this, toggleTransition)
        if (isRepeated) {
            repetitionConfigGroup.visibility = View.VISIBLE
            timeSpanCheckBox.isChecked = currentCondition?.intervalIsHourRangeUsed ?: false
            timeSpanCheckBox.isEnabled = true

        } else {
            repetitionConfigGroup.visibility = View.GONE
            val rangeOriginallyUsed = currentCondition?.intervalIsHourRangeUsed ?: false
            timeSpanCheckBox.isChecked = false
            if (rangeOriginallyUsed) {
                currentCondition?.intervalIsHourRangeUsed = true
            }
            timeSpanCheckBox.isEnabled = false
        }
    }


    private fun applyConfigMode(mode: Byte, animate: Boolean) {
        refreshingViews = true
        if (animate) {
            TransitionManager.beginDelayedTransition(this, modeChangeTransition)
        }

        when (mode) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {
                configTypePropertyView.value = 0
                alarmConfigGroup.visibility = VISIBLE
                intervalConfigGroup.visibility = GONE
                ui_ema_group.visibility = GONE

                isRepeatedView.locked = false
                isRepeatedView.alpha = 1.0f
            }

            OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                configTypePropertyView.value = 1
                alarmConfigGroup.visibility = GONE
                ui_ema_group.visibility = GONE
                intervalConfigGroup.visibility = VISIBLE

                isRepeatedView.locked = false
                isRepeatedView.alpha = 1.0f
            }

            OTTimeTriggerCondition.TIME_CONDITION_SAMPLING -> {
                configTypePropertyView.value = 2

                alarmConfigGroup.visibility = GONE
                intervalConfigGroup.visibility = GONE
                ui_ema_group.visibility = View.VISIBLE

                if (currentCondition?.isRepeated != true) {
                    currentCondition?.isRepeated = true
                    applyIsRepeated(true, false)
                    notifyConditionChanged()
                }
                isRepeatedView.locked = true
                isRepeatedView.alpha = 0.3f
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
                    2 -> OTTimeTriggerCondition.TIME_CONDITION_SAMPLING
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

    override fun onCheckedChanged(view: CompoundButton, isChecked: Boolean) {
        if (view === timeSpanCheckBox) {
            TransitionManager.beginDelayedTransition(this, toggleTransition)
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
            TransitionManager.beginDelayedTransition(this, toggleTransition)
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
        } else if (view === ui_ema_use_sampling_range) {
            TransitionManager.beginDelayedTransition(this, toggleTransition)
            if (isChecked) {
                ui_ema_range_picker.visibility = View.VISIBLE
            } else {
                ui_ema_range_picker.visibility = View.GONE
            }

            if (currentCondition?.samplingRangeUsed != isChecked) {
                currentCondition?.samplingRangeUsed = isChecked
                notifyConditionChanged()
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

            ui_ema_count.value = condition.samplingCount.toInt()
            ui_ema_use_sampling_range.isChecked = condition.samplingRangeUsed

            ui_ema_minimum_interval_picker.durationSeconds = condition.samplingMinIntervalSeconds.toInt()

            ui_ema_range_picker.fromHourOfDay = condition.samplingHourStart.toInt()
            ui_ema_range_picker.toHourOfDay = condition.samplingHourEnd.toInt()

            suspendConditionChangeEvent = false
        }
    }

    private fun notifyConditionChanged() {
        if (!suspendConditionChangeEvent)
            currentCondition?.let { this.conditionChanged.onNext(it) }
    }
}