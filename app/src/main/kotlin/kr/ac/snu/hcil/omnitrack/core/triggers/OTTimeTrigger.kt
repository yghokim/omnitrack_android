package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.receivers.OTSystemReceiver
import kr.ac.snu.hcil.omnitrack.utils.*
import java.util.*

/**
 * Created by younghokim on 16. 7. 27..
 */
class OTTimeTrigger : OTTrigger {

    companion object {
        const val CONFIG_TYPE_ALARM = 0
        const val CONFIG_TYPE_INTERVAL = 1

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"

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
         * 7 bits : days of week flags
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
            return BitwiseOperationHelper.getBooleanAt(range, DAYS_OF_WEEK_FLAGS_SHIFT + (6 - dayOfWeek))
        }

        fun getAllDayOfWeekFlags(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, DAYS_OF_WEEK_FLAGS_SHIFT, DAYS_OF_WEEK_FLAGS_MASK)
        }

        fun setIsDayOfWeekUsed(range: Int, dayOfWeek: Int, isUsed: Boolean): Int {
            return BitwiseOperationHelper.setBooleanAt(range, isUsed, DAYS_OF_WEEK_FLAGS_SHIFT + (6 - dayOfWeek))
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

    override val typeNameResourceId: Int = R.string.trigger_periodic_name
    override val descriptionResourceId: Int = R.string.trigger_periodic_desc

    var configType: Int by ObservableMapDelegate(CONFIG_TYPE_ALARM, properties) {
        isDirtySinceLastSync = true
        onConfigChanged()
    }

    var rangeVariables: Int by ObservableMapDelegate(0, properties) {
        isDirtySinceLastSync = true
        onRangeChanged()
    }

    private val configVariablesDelegate = ObservableMapDelegate<OTTimeTrigger, Int>(AlarmConfig.makeConfig(9, 0, 1), properties) {
        isDirtySinceLastSync = true
        onConfigChanged()
    }
    var configVariables: Int by configVariablesDelegate

    private val isRepeatedDelegate = ObservableMapDelegate<OTTimeTrigger, Int>(0, properties) {
        isDirtySinceLastSync = true
        onRangeChanged()
    }
    private var _isRepeated: Int by isRepeatedDelegate


    var isRepeated: Boolean
        get() {
            println(properties)
            println(isRepeatedDelegate)
            return if (_isRepeated > 0) {
                true
            } else false
        }
        set(value) {
            _isRepeated = if (value == true) 1 else 0
        }


    private var cacheCalendar = Calendar.getInstance()
    private var cacheCalendar2 = Calendar.getInstance()


    constructor(objectId: String?, dbId: Long?, name: String, trackerObjectId: String, isOn: Boolean, lastTriggeredTime: Long, serializedProperties: String? = null) : super(objectId, dbId, name, trackerObjectId, isOn, lastTriggeredTime, serializedProperties) {

    }

    private fun makeIntent(context: Context, triggerTime: Long, alarmId: Int): PendingIntent {
        val intent = Intent(context, OTSystemReceiver::class.java)
        intent.action = OTApplication.BROADCAST_ACTION_ALARM
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER, this.objectId)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, OTApplication.app.currentUser.objectId)
        intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)

        return PendingIntent.getBroadcast(context, alarmId, intent, 0)
    }

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

            val limitExclusive = if (isRepeated) {
                cacheCalendar.set(Range.getEndYear(rangeVariables), Range.getEndZeroBasedMonth(rangeVariables), Range.getEndDay(rangeVariables) + 1)
                cacheCalendar.timeInMillis
            } else Long.MAX_VALUE

            val intrinsicNext = when (configType) {
                CONFIG_TYPE_ALARM -> {

                    cacheCalendar.timeInMillis = now
                    cacheCalendar2.set(0, 0, 0, AlarmConfig.getHourOfDay(configVariables), AlarmConfig.getMinute(configVariables), 0)
                    cacheCalendar2.set(Calendar.YEAR, cacheCalendar.getYear())
                    cacheCalendar2.set(Calendar.MONTH, cacheCalendar.getZeroBasedMonth())
                    cacheCalendar2.set(Calendar.DAY_OF_MONTH, cacheCalendar.getDayOfMonth())

                    if (!isRepeated) {
                        if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {

                            if (TimeHelper.compareTimePortions(cacheCalendar, cacheCalendar2) >= -MILLISECOND_TOLERANCE) {
                                //next day
                                cacheCalendar2.add(Calendar.DAY_OF_YEAR, 1)

                            }

                            cacheCalendar2.timeInMillis
                        } else 0L
                    } else if (!Range.isAllDayNotUsed(rangeVariables)) {
                        //repetition

                        var closestDayOfWeek = cacheCalendar2.getDayOfWeek() // today
                        while (!Range.isDayOfWeekUsed(rangeVariables, closestDayOfWeek)) {
                            closestDayOfWeek = (closestDayOfWeek + 1) % 7
                        }

                        val leftDays = if (closestDayOfWeek == cacheCalendar2.getDayOfWeek() && TimeHelper.compareTimePortions(cacheCalendar, cacheCalendar2) >= -MILLISECOND_TOLERANCE) {
                            7
                        } else TimeHelper.getDaysLeftToClosestDayOfWeek(cacheCalendar2, closestDayOfWeek)

                        cacheCalendar2.add(Calendar.DAY_OF_YEAR, leftDays)
                        cacheCalendar2.timeInMillis
                    } else 0L
                }
                CONFIG_TYPE_INTERVAL -> {
                    val intervalMillis = IntervalConfig.getIntervalSeconds(configVariables) * 1000
                    val startBoundHourOfDay = IntervalConfig.getStartHour(configVariables)
                    val endBoundHourOfDay = IntervalConfig.getEndHour(configVariables)

                    val realPivot = if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                        now
                    } else pivot


                    //extract hour range of today
                    cacheCalendar.timeInMillis = now

                    cacheCalendar.setHourOfDay(startBoundHourOfDay, cutUnder = true)
                    val lowerBoundToday = cacheCalendar.timeInMillis

                    cacheCalendar.setHourOfDay(endBoundHourOfDay, cutUnder = true)
                    if (startBoundHourOfDay >= endBoundHourOfDay) {
                        cacheCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val upperBoundToday = cacheCalendar.timeInMillis

                    val upperBoundLastDay = TimeHelper.addDays(upperBoundToday, -1)


                    val intrinsicNextTime =
                            if (now < upperBoundLastDay) {
                                if (isRepeated) {
                                    cacheCalendar.timeInMillis = now
                                    cacheCalendar.add(Calendar.DAY_OF_YEAR, -1)
                                    if (Range.isDayOfWeekUsed(rangeVariables, cacheCalendar.getDayOfWeek())) {
                                        //yesterday is used
                                        val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                        if (intrinsic < upperBoundLastDay) {
                                            intrinsic
                                        } else {
                                            getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                        }
                                    } else {
                                        getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                    }
                                } else {

                                    val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                    if (intrinsic < upperBoundLastDay) {
                                        intrinsic
                                    } else {
                                        if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                            lowerBoundToday
                                        } else 0L
                                    }
                                }
                            } else if (upperBoundLastDay <= now && now < lowerBoundToday) {
                                if (isRepeated) {
                                    getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                } else {
                                    if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                        lowerBoundToday
                                    } else {
                                        0L
                                    }
                                }
                            } else if (lowerBoundToday <= now && now < upperBoundToday) {
                                //now is later than upperBound
                                if (isRepeated) {
                                    cacheCalendar.timeInMillis = now
                                    if (Range.isDayOfWeekUsed(rangeVariables, cacheCalendar.getDayOfWeek())) {
                                        //yesterday is used
                                        val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                        if (intrinsic < upperBoundToday) {
                                            intrinsic
                                        } else {
                                            getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                        }
                                    } else {
                                        getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                    }
                                } else {
                                    val intrinsic = getIntrinsicNextIntervalTime(realPivot, now, intervalMillis)
                                    if (intrinsic < upperBoundToday) {
                                        intrinsic
                                    } else {
                                        if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                            TimeHelper.addDays(lowerBoundToday, 1)
                                        } else 0L
                                    }
                                }
                            } else {
                                if (isRepeated) {
                                    getClosestLowerBoundOfInterval(now, startBoundHourOfDay)
                                } else {
                                    if (pivot == TRIGGER_TIME_NEVER_TRIGGERED) {
                                        TimeHelper.addDays(lowerBoundToday, 1)
                                    } else {
                                        0L
                                    }
                                }
                            }
                    intrinsicNextTime
                }
                else -> 0L
            }

            if (intrinsicNext < limitExclusive) {
                return intrinsicNext
            } else {
                return 0
            }

        } else return 0
    }

    private fun getIntrinsicNextIntervalTime(pivot: Long, now: Long, interval: Int): Long {
        val skipIntervalCount = (now - pivot) / interval

        println("$skipIntervalCount intervals skipped.")

        return pivot + (skipIntervalCount + 1) * interval
    }


    private fun getClosestLowerBoundOfInterval(from: Long, lowerBoundHourOfDay: Int): Long {
        cacheCalendar.timeInMillis = from

        val pivotDayOfWeek = cacheCalendar.getDayOfWeek()


        if (Range.isDayOfWeekUsed(rangeVariables, pivotDayOfWeek) && cacheCalendar.getHourOfDay() < lowerBoundHourOfDay) {
            cacheCalendar.timeInMillis = from
            cacheCalendar.set(Calendar.HOUR_OF_DAY, lowerBoundHourOfDay)
            return cacheCalendar.timeInMillis
        } else {
            var daysLeft = 1
            while (!Range.isDayOfWeekUsed(rangeVariables, (pivotDayOfWeek + daysLeft) % 7)) {
                daysLeft++
            }

            cacheCalendar.timeInMillis = from
            cacheCalendar.set(Calendar.HOUR_OF_DAY, lowerBoundHourOfDay)
            cacheCalendar.add(Calendar.DAY_OF_YEAR, daysLeft)
            return cacheCalendar.timeInMillis
        }
    }

    private fun onConfigChanged() {

        val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(makeIntent(OTApplication.app, 0, 0))
        if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
            isOn = false
        }
    }

    private fun onRangeChanged() {
        val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(makeIntent(OTApplication.app, 0, 0))
        if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
            isOn = false
        }
    }


    override fun handleActivationOnSystem(context: Context) {
        println("trigger activated")
        if (isOn) {
            //TODO need to check current alarmmanager to avoid duplicate alarm
            if (!reserveNextAlarmToSystem(lastTriggeredTime)) {
                isOn = false
            }
        }
    }

    private fun reserveNextAlarmToSystem(currentTriggerTime: Long): Boolean {

        val nextAlarmTime = getNextAlarmTime(currentTriggerTime)

        if (nextAlarmTime > 0L) {

            cacheCalendar.timeInMillis = nextAlarmTime
            println("next alarm will be fired at $cacheCalendar")

            val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.set(AlarmManager.RTC, nextAlarmTime, makeIntent(OTApplication.app, nextAlarmTime, 0))
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
        val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(makeIntent(OTApplication.app, 0, 0))
    }
}