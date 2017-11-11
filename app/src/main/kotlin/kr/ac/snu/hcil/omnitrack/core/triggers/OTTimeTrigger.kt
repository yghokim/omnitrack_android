package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import android.util.Log
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.OTTriggerAlarmInstance
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper
import kr.ac.snu.hcil.omnitrack.utils.ObservableMapDelegate
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.Time
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 27
 */
class OTTimeTrigger(objectId: String?, user: OTUser, name: String, trackerObjectIds: Array<String>?, isOn: Boolean, action: Int, lastTriggeredTime: Long?, propertyData: Map<String, String>? = null) : OTTrigger(objectId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, propertyData) {

    companion object {

        const val TAG = "TimeTrigger"

        const val CONFIG_TYPE_ALARM = 0
        const val CONFIG_TYPE_INTERVAL = 1

        fun configIconId(configType: Int): Int {
            return when (configType) {
                CONFIG_TYPE_ALARM -> R.drawable.alarm_dark
                CONFIG_TYPE_INTERVAL -> R.drawable.repeat_dark
                else -> R.drawable.alarm_dark
            }
        }

        fun configNameId(configType: Int): Int {
            return when (configType) {
                CONFIG_TYPE_ALARM -> R.string.msg_trigger_time_config_desc_alarm
                CONFIG_TYPE_INTERVAL -> R.string.msg_trigger_time_config_desc_interval
                else -> R.string.msg_trigger_time_config_desc_alarm
            }
        }
    }


    open class Config {
        // 1 bottom bit is specified flag
        companion object {
            const val IS_SPECIFIED_SHIFT = 0
        }

        fun isSpecified(config: Int): Boolean {
            return BitwiseOperationHelper.getBooleanAt(config, IS_SPECIFIED_SHIFT)
        }

        fun setIsSpecified(config: Int, specified: Boolean): Int {
            return BitwiseOperationHelper.setBooleanAt(config, specified, IS_SPECIFIED_SHIFT)
        }

        open fun toHumanReadableString(config: Int): String {
            return Integer.toBinaryString(config)
        }
    }

    object Range : Config() {
        /**32bit Integer
         * 7 bits : days of week flags.
         * 1 bit: isLimitDateSpecified
         * 5bit:  years from 2016 ( max +32)
         * 4bit: zeroBasedMonth (0~11)
         * 5bit: dayOfMonth(1~31)
         */

        const val DAYS_OF_WEEK_FLAGS_MASK = 0b1111111 //1111111

        const val YEAR_MASK = 0b11111 //1111111
        const val MONTH_MASK = 0b1111
        const val DAY_MASK = 0b11111

        const val DAYS_OF_WEEK_FLAGS_SHIFT = 25
        const val IS_END_SPECIFIED_SHIFT = 24
        const val YEAR_SHIFT = 19
        const val MONTH_SHIFT = 15
        const val DAY_SHIFT = 10

        fun isAllDayUsed(range: Int): Boolean {
            return BitwiseOperationHelper.getIntAt(range, DAYS_OF_WEEK_FLAGS_SHIFT, DAYS_OF_WEEK_FLAGS_MASK) == DAYS_OF_WEEK_FLAGS_MASK
        }

        fun isAllDayNotUsed(range: Int): Boolean {
            return BitwiseOperationHelper.getIntAt(range, DAYS_OF_WEEK_FLAGS_SHIFT, DAYS_OF_WEEK_FLAGS_MASK) == 0
        }


        fun isDayOfWeekUsed(range: Int, dayOfWeek: Int): Boolean {
            return BitwiseOperationHelper.getBooleanAt(range, DAYS_OF_WEEK_FLAGS_SHIFT + (7 - dayOfWeek))
        }

        fun getAllDayOfWeekFlags(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, DAYS_OF_WEEK_FLAGS_SHIFT, DAYS_OF_WEEK_FLAGS_MASK)
        }

        fun getEndYear(range: Int): Int {
            return 2016 + BitwiseOperationHelper.getIntAt(range, YEAR_SHIFT, YEAR_MASK)
        }

        fun setEndYear(range: Int, year: Int): Int {
            if (year - 2016 < 0 || year - 2016 > YEAR_MASK) {
                throw Exception("year range exceeded.")
            }

            return BitwiseOperationHelper.setIntAt(range, year - 2016, YEAR_SHIFT, YEAR_MASK)
        }

        fun getEndZeroBasedMonth(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, MONTH_SHIFT, MONTH_MASK)
        }

        fun setEndZeroBasedMonth(range: Int, month: Int): Int {

            return BitwiseOperationHelper.setIntAt(range, month, MONTH_SHIFT, MONTH_MASK)
        }

        fun getEndDay(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, DAY_SHIFT, DAY_MASK)
        }

        fun setEndDay(range: Int, day: Int): Int {

            return BitwiseOperationHelper.setIntAt(range, day, DAY_SHIFT, DAY_MASK)
        }

        fun isEndSpecified(range: Int): Boolean {
            return BitwiseOperationHelper.getBooleanAt(range, IS_END_SPECIFIED_SHIFT)
        }

        fun makeConfig(dayOfWeekFlags: Int): Int {
            return (1 shl IS_SPECIFIED_SHIFT) or (dayOfWeekFlags shl DAYS_OF_WEEK_FLAGS_SHIFT) or
                    (0 shl IS_END_SPECIFIED_SHIFT)
        }

        fun makeConfig(dayOfWeekFlags: Int, endYear: Int, endMonth: Int, endDay: Int): Int {
            return (1 shl IS_SPECIFIED_SHIFT) or (dayOfWeekFlags shl DAYS_OF_WEEK_FLAGS_SHIFT) or
                    (1 shl IS_END_SPECIFIED_SHIFT) or
                    ((endYear - 2016) shl YEAR_SHIFT) or (endMonth shl MONTH_SHIFT) or (endDay shl DAY_SHIFT)
        }
    }

    object AlarmConfig : Config() {

        /*
            32bit integer
            1bit : ampm (0~1)
            4bits : hour (0~11)
            6bits : minute(0~59)

            last 1 bit : isSpecified
         */
        const val AM_PM_MASK = 0b1
        const val HOUR_MASK = 0b1111
        const val MINUTE_MASK = 0b111111

        const val AM_PM_SHIFT = 31
        const val HOUR_SHIFT = 27
        const val MINUTE_SHIFT = 21

        fun getAlarmTimeConfig(config: Int): Time {
            return Time(getHourOfDay(config), getMinute(config), 0)
        }

        fun getHour(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, HOUR_SHIFT, HOUR_MASK)
        }

        fun getHourOfDay(config: Int): Int {
            return getHour(config) + 12 * getAmPm(config)
        }

        fun setHour(config: Int, hour: Int): Int {
            return BitwiseOperationHelper.setIntAt(config, hour, HOUR_SHIFT, HOUR_MASK)
        }

        fun getMinute(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, MINUTE_SHIFT, MINUTE_MASK)
        }

        fun setMinute(config: Int, minute: Int): Int {
            return BitwiseOperationHelper.setIntAt(config, minute, MINUTE_SHIFT, MINUTE_MASK)
        }

        fun getAmPm(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, AM_PM_SHIFT, AM_PM_MASK)
        }

        fun setAmPm(config: Int, isPm: Boolean): Int {
            return BitwiseOperationHelper.setBooleanAt(config, isPm, AM_PM_SHIFT)
        }

        fun makeConfig(hour: Int, minute: Int, amPm: Int): Int {
            return (1 shl IS_SPECIFIED_SHIFT) or
                    (hour shl HOUR_SHIFT) or
                    (minute shl MINUTE_SHIFT) or
                    (amPm shl AM_PM_SHIFT)
        }

        override fun toHumanReadableString(config: Int): String {
            return super.toHumanReadableString(config) + " - hour: ${getHour(config)}, minute: ${getMinute(config)}, amPm: ${getAmPm(config)}"
        }
    }

    object IntervalConfig : Config() {

        /*

        32 bit integer
            16 bits: seconds
            5 bits : time of day(0~24) - start
            5 bits : time of day(0~24) - end
            1 bit : flag to use the hours range
         */

        const val DURATION_SHIFT = 16
        const val TIME_OF_DAY_START_SHIFT = 11
        const val TIME_OF_DAY_END_SHIFT = 6


        const val TIME_MASK = 0xFFFF
        const val TIME_OF_DAY_MASK = 0b11111 // 11111

        fun getIntervalSeconds(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, DURATION_SHIFT, TIME_MASK)
        }

        fun setIntervalSeconds(config: Int, interval: Int): Int {
            return BitwiseOperationHelper.setIntAt(config, interval, DURATION_SHIFT, TIME_MASK)
        }

        fun getStartHour(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, TIME_OF_DAY_START_SHIFT, TIME_OF_DAY_MASK)
        }

        fun getEndHour(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, TIME_OF_DAY_END_SHIFT, TIME_OF_DAY_MASK)
        }

        fun makeConfig(durationSeconds: Int): Int {
            return makeConfig(durationSeconds, 0, 0)
        }

        fun makeConfig(durationSeconds: Int, startHour: Int, endHour: Int): Int {
            return (1 shl IS_SPECIFIED_SHIFT) or (durationSeconds shl DURATION_SHIFT) or (startHour shl TIME_OF_DAY_START_SHIFT) or (endHour shl TIME_OF_DAY_END_SHIFT)
        }

        override fun toHumanReadableString(config: Int): String {
            return super.toHumanReadableString(config) + "| duration seconds : ${getIntervalSeconds(config)}"
        }
    }

    override val configIconId: Int get() = configIconId(configType)

    override val configTitleId: Int get() = configNameId(configType)

    override val typeId: Int = TYPE_TIME

    override val typeNameResourceId: Int = R.string.trigger_name_time
    override val descriptionResourceId: Int = R.string.trigger_desc_time

    var configType: Int by ObservableMapDelegate(CONFIG_TYPE_ALARM, properties) {
        value ->
        syncPropertyToDatabase("configType", value)
        onConfigChanged()
        notifyPropertyChanged("configType", value)
    }

    var rangeVariables: Int by ObservableMapDelegate(Range.makeConfig(Range.DAYS_OF_WEEK_FLAGS_MASK), properties) {
        value ->
        syncPropertyToDatabase("rangeVariables", value)
        onRangeChanged()
        notifyPropertyChanged("rangeVariables", value)
    }

    private val configVariablesDelegate = ObservableMapDelegate<OTTimeTrigger, Int>(AlarmConfig.makeConfig(9, 0, 1), properties) {
        value ->
        syncPropertyToDatabase("configVariables", value)
        onConfigChanged()
        notifyPropertyChanged("configVariables", value)
    }
    var configVariables: Int by configVariablesDelegate

    private val repeatedDelegate = ObservableMapDelegate<OTTimeTrigger, Int>(1, properties) {
        value ->
        syncPropertyToDatabase("repeated", value)
        onRangeChanged()
        notifyPropertyChanged("isRepeated", value)
    }
    private var repeated: Int by repeatedDelegate


    var isRepeated: Boolean
        get() {
            return repeated > 0
        }
        set(value) {
            repeated = if (value == true) 1 else 0
        }


    private var cacheCalendar = Calendar.getInstance()

    val isRangeSpecified: Boolean get() = OTTimeTrigger.Range.isSpecified(rangeVariables)
    val isConfigSpecified: Boolean get() {
        return when (configType) {
            CONFIG_TYPE_ALARM -> AlarmConfig.isSpecified(configVariables)
            CONFIG_TYPE_INTERVAL -> IntervalConfig.isSpecified(configVariables)
            else -> false
        }
    }

    val onAlarmReserved = PublishSubject.create<Long?>()

    fun getNextAlarmTime(pivot: Long?): Long? {
        if (isOn) {

            val now = System.currentTimeMillis()

            val limitExclusive = if (isRepeated && Range.isEndSpecified(rangeVariables)) {
                cacheCalendar.set(Range.getEndYear(rangeVariables), Range.getEndZeroBasedMonth(rangeVariables), Range.getEndDay(rangeVariables) + 1)
                cacheCalendar.timeInMillis
            } else Long.MAX_VALUE

            val nextTimeCalculator = when (configType) {
                CONFIG_TYPE_ALARM -> {

                    val calculator = DesignatedTimeScheduleCalculator().setAlarmTime(AlarmConfig.getHourOfDay(configVariables), AlarmConfig.getMinute(configVariables), 0)
                    if (isRepeated) {
                        calculator.setAvailableDaysOfWeekFlag(Range.getAllDayOfWeekFlags(rangeVariables))
                                .setEndAt(limitExclusive)
                    }

                    calculator
                }
                CONFIG_TYPE_INTERVAL -> {
                    val intervalMillis = IntervalConfig.getIntervalSeconds(configVariables) * 1000
                    val startBoundHourOfDay = IntervalConfig.getStartHour(configVariables)
                    val endBoundHourOfDay = IntervalConfig.getEndHour(configVariables)

                    val calculator = IntervalTimeScheduleCalculator().setInterval(intervalMillis.toLong())

                    if (isRepeated) {
                        calculator
                                .setHourBoundingRange(startBoundHourOfDay, endBoundHourOfDay)
                                .setAvailableDaysOfWeekFlag(Range.getAllDayOfWeekFlags(rangeVariables))
                                .setEndAt(limitExclusive)
                    } else {
                        calculator.setNoLimit()
                    }

                    calculator
                }
                else -> throw Exception("Unsupported Time Config Type")
            }
            return nextTimeCalculator.calculateNext(pivot, now)

        } else return null
    }


    private fun onConfigChanged() {
        OTApp.logger.writeSystemLog("Time trigger config changed. cancel trigger", TAG)

        //OTApp.instance.triggerAlarmManager.cancelTrigger(this)
        if (reserveNextAlarmToSystem(lastTriggeredTime) == null) {
            isOn = false
        }
    }

    private fun onRangeChanged() {

        OTApp.logger.writeSystemLog("Time trigger range changed. cancel trigger", TAG)

        //OTApp.instance.triggerAlarmManager.cancelTrigger(this)
        if (reserveNextAlarmToSystem(lastTriggeredTime) == null) {
            isOn = false
        }
    }


    override fun handleActivationOnSystem(context: Context) {

        if (isOn == false) {
            handleOff()
        } else {
            if (OTApp.instance.triggerAlarmManager.getNearestAlarmTime(this, System.currentTimeMillis()) == null) {
                if (reserveNextAlarmToSystem(lastTriggeredTime) == null) {
                    isOn = false
                }
            }
        }
    }

    fun reserveNextAlarmToSystem(currentTriggerTime: Long?): OTTriggerAlarmInstance.AlarmInfo? {

        println("current trigger time: ${currentTriggerTime}")

        val nextAlarmTime = getNextAlarmTime(currentTriggerTime)

        if (nextAlarmTime != null) {

            cacheCalendar.timeInMillis = nextAlarmTime
            println("next alarm will be fired at $cacheCalendar")

            OTApp.logger.writeSystemLog("Next alarm is reserved at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(nextAlarmTime))}", TAG)

            //val nextAlarmInfo = OTApp.instance.triggerAlarmManager.reserveAlarm(this, nextAlarmTime, !isRepeated)
            //onAlarmReserved.onNext(nextAlarmInfo.reservedAlarmTime)
            return /*nextAlarmInfo*/ null
        } else {
            println("Finish trigger. Do not repeat.")
            onAlarmReserved.onNext(/*null*/0)
            return null
        }
    }

    override fun handleFire(triggerTime: Long) {

    }

    override fun handleOn() {
        Log.d(TAG, "handle Time trigger on.")
        if (reserveNextAlarmToSystem(null) == null) {
            isOn = false
        }
    }

    override fun handleOff() {
        Log.d(TAG, "handle Time trigger off.")
        lastTriggeredTime = null
        OTApp.logger.writeSystemLog("Time trigger turned off. cancel trigger", TAG)
        //TApp.instance.triggerAlarmManager.cancelTrigger(this)
    }

    override fun onDetachFromSystem() {
        //OTApp.instance.triggerAlarmManager.cancelTrigger(this)
    }
}