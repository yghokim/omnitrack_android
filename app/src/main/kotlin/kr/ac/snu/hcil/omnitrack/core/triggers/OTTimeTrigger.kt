package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper
import kr.ac.snu.hcil.omnitrack.utils.ObservableMapDelegate
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 7. 27
 */
class OTTimeTrigger(objectId: String?, user: OTUser, name: String, trackerObjectIds: Array<String>?, isOn: Boolean, action: Int, lastTriggeredTime: Long, propertyData: Map<String, String>? = null) : OTTrigger(objectId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, propertyData) {

    companion object {

        const val TAG = "TimeTrigger"

        const val CONFIG_TYPE_ALARM = 0
        const val CONFIG_TYPE_INTERVAL = 1

        const val MILLISECOND_TOLERANCE = 1000L

        fun configIconId(configType: Int): Int {
            return when (configType) {
                CONFIG_TYPE_ALARM -> R.drawable.alarm_dark
                CONFIG_TYPE_INTERVAL -> R.drawable.repeat_dark
                else -> R.drawable.alarm_dark
            }
        }

        fun configNameId(configType: Int): Int {
            return when (configType) {
                CONFIG_TYPE_ALARM -> R.string.msg_trigger_time_config_type_name_alram
                CONFIG_TYPE_INTERVAL -> R.string.msg_trigger_time_config_type_name_interval
                else -> R.string.msg_trigger_time_config_type_name_alram
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
    private var cacheCalendar2 = Calendar.getInstance()


    val isRangeSpecified: Boolean get() = OTTimeTrigger.Range.isSpecified(rangeVariables)
    val isConfigSpecified: Boolean get() {
        return when (configType) {
            CONFIG_TYPE_ALARM -> AlarmConfig.isSpecified(configVariables)
            CONFIG_TYPE_INTERVAL -> IntervalConfig.isSpecified(configVariables)
            else -> false
        }
    }

    /*

    val isTriggeredOnce: Boolean get() {
        if (isRangeSpecified) {
            return BitwiseOperationHelper.getIntAt(rangeVariables, Range.DAYS_OF_WEEK_FLAGS_SHIFT, Range.DAYS_OF_WEEK_FLAGS_MASK) == 0
        } else return true
    }*/

    fun getNextAlarmTime(pivot: Long): Long {
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


            val intrinsicNext = nextTimeCalculator.calculateNext(if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                null
            } else pivot, now) ?: 0

            if (intrinsicNext < limitExclusive) {
                return intrinsicNext
            } else {
                return 0
            }

        } else return 0
    }


    private fun onConfigChanged() {
        OTApplication.logger.writeSystemLog("Time trigger config changed. cancel trigger", TAG)
        //val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        OTApplication.app.timeTriggerAlarmManager.cancelTrigger(this)
        if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
            isOn = false
        }
    }

    private fun onRangeChanged() {
        //val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        OTApplication.logger.writeSystemLog("Time trigger range changed. cancel trigger", TAG)

        OTApplication.app.timeTriggerAlarmManager.cancelTrigger(this)
        if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
            isOn = false
        }
    }


    override fun handleActivationOnSystem(context: Context) {
        println("trigger activated")
        if (isOn) {
            //TODO need to check current alarmmanager to avoid duplicate alarm
            if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
                println("didn't reserve next alarm. turn off")
                isOn = false
            } else {
                println("reserved next alarm.")
            }
        }
    }

    private fun reserveNextAlarmToSystem(currentTriggerTime: Long): Boolean {

        val nextAlarmTime = getNextAlarmTime(currentTriggerTime)

        if (nextAlarmTime > 0L) {

            cacheCalendar.timeInMillis = nextAlarmTime
            println("next alarm will be fired at $cacheCalendar")

            OTApplication.logger.writeSystemLog("Next alarm is reserved at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(nextAlarmTime))}", TAG)

            OTApplication.app.timeTriggerAlarmManager.reserveAlarm(this, nextAlarmTime)
            return true
        } else {
            println("Finish trigger. Do not repeat.")
            return false
        }
    }

    override fun handleFire(triggerTime: Long) {
        if (!reserveNextAlarmToSystem(triggerTime)) {
            isOn = false
        }
    }

    override fun handleOn() {
        if (!reserveNextAlarmToSystem(TRIGGER_TIME_NEVER_TRIGGERED)) {
            isOn = false
        }
    }

    override fun handleOff() {

        OTApplication.logger.writeSystemLog("Time trigger turned off. cancel trigger", TAG)
        OTApplication.app.timeTriggerAlarmManager.cancelTrigger(this)
    }

    override fun onDetachFromSystem() {
        OTApplication.app.timeTriggerAlarmManager.cancelTrigger(this)
    }
}