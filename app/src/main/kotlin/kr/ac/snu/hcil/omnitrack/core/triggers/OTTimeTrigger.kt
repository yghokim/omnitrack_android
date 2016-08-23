package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.receivers.OTSystemReceiver
import kr.ac.snu.hcil.omnitrack.utils.BitwiseOperationHelper

/**
 * Created by younghokim on 16. 7. 27..
 */
class OTTimeTrigger : OTTrigger {

    companion object {
        const val CONFIG_TYPE_ALARM = 0
        const val CONFIG_TYPE_INTERVAL = 1
    }


    open class Config {
        // 1 top bit is specified flag
        companion object {
            const val IS_SPECIFIED_SHIFT = 31
        }

        fun isSpecified(config: Int): Boolean {
            return BitwiseOperationHelper.getBooleanAt(config, IS_SPECIFIED_SHIFT)
        }

        fun setIsSpecified(config: Int, specified: Boolean): Int {
            return BitwiseOperationHelper.setBooleanAt(config, specified, IS_SPECIFIED_SHIFT)
        }
    }

    object Range : Config() {
        /**32bit Integer
         * 1 bit : isRangeUsed flag
         * 7 bits : days of week flags
         * 5 bits : time of day(0~24) - start
         * 5 bits : time of day(0~24) - end
         *
         */
        const val DAYS_OF_WEEK_FLAGS_MASK = 0b1111111 //1111111
        const val TIME_OF_DAY_MASK = 0b11111 // 11111

        const val DAYS_OF_WEEK_FLAGS_SHIFT = 24
        const val TIME_OF_DAY_START_SHIFT = 19
        const val TIME_OF_DAY_END_SHIFT = 14


        fun isDayOfWeekUsed(range: Int, dayOfWeek: Int): Boolean {
            return BitwiseOperationHelper.getBooleanAt(range, DAYS_OF_WEEK_FLAGS_SHIFT + (7 - dayOfWeek))
        }

        fun setIsDayOfWeekUsed(range: Int, dayOfWeek: Int, isUsed: Boolean): Int {
            return BitwiseOperationHelper.setBooleanAt(range, isUsed, DAYS_OF_WEEK_FLAGS_SHIFT + (7 - dayOfWeek))
        }

        fun getStartHour(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, TIME_OF_DAY_START_SHIFT, TIME_OF_DAY_MASK)
        }

        fun getEndHour(range: Int): Int {
            return BitwiseOperationHelper.getIntAt(range, TIME_OF_DAY_END_SHIFT, TIME_OF_DAY_MASK)
        }
    }

    object AlarmConfig : Config() {

        /*
            32bit integer
            1bit : isSpecified
            1bit : ampm (0~1)
            4bits : hour (0~11)
            6bits : minute(0~59)
         */
        const val AM_PM_MASK = 0b1
        const val HOUR_MASK = 0b11111
        const val MINUTE_MASK = 0b111111

        const val AM_PM_SHIFT = 30
        const val HOUR_SHIFT = 26
        const val MINUTE_SHIFT = 20


        fun getHour(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, HOUR_SHIFT, HOUR_MASK)
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
    }

    object IntervalConfig : Config() {

        /*

        32 bit integer
            16 bits: seconds
         */

        const val TIME_SHIFT = 17

        const val TIME_MASK = 0xFFFF


        fun getIntervalSeconds(config: Int): Int {
            return BitwiseOperationHelper.getIntAt(config, TIME_SHIFT, TIME_MASK)
        }

        fun setIntervalSeconds(config: Int, interval: Int): Int {
            return BitwiseOperationHelper.setIntAt(config, interval, TIME_SHIFT, TIME_MASK)
        }

        fun makeConfig(hours: Int, minutes: Int, seconds: Int): Int {
            return 1 shl IS_SPECIFIED_SHIFT or (hours * 3600 + minutes * 60 + seconds) shl TIME_SHIFT
        }
    }

    override val typeId: Int = TYPE_TIME

    override val typeNameResourceId: Int = R.string.trigger_periodic_name
    override val descriptionResourceId: Int = R.string.trigger_periodic_desc

    var configType: Int by properties

    var rangeVariables: Int by properties
    var configVariables: Int by properties

    constructor(objectId: String?, dbId: Long?, name: String, trackerObjectId: String, isOn: Boolean, serializedProperties: String? = null) : super(objectId, dbId, name, trackerObjectId, isOn, serializedProperties) {
        configType = CONFIG_TYPE_ALARM
        rangeVariables = 0
        configVariables = 0
    }

    private fun makeIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, OTSystemReceiver::class.java)
        intent.action = OTApplication.BROADCAST_ACTION_ALARM
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER, this.objectId)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, OTApplication.app.currentUser.objectId)

        return PendingIntent.getBroadcast(context, alarmId, intent, 0)
    }

    fun getNextAlarmTime(): Long {
        if (isOn) {
            return System.currentTimeMillis() + 1000
        } else return 0
    }

    override fun handleActivationOnSystem(context: Context) {
        println("trigger activated")
        if (isOn) {
            setNextAlarm()
        }
    }

    private fun setNextAlarm(): Boolean {

        val nextAlarmTime = getNextAlarmTime()

        if (nextAlarmTime > 0L) {

            println("next alarm will be fired at $nextAlarmTime")

            val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.set(AlarmManager.RTC, nextAlarmTime, makeIntent(OTApplication.app, 0))
            return true
        } else {
            return false
        }
    }

    override fun handleFire() {
        setNextAlarm()
    }

    override fun handleOn() {
        setNextAlarm()
    }

    override fun handleOff() {
        val alarmManager = OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(makeIntent(OTApplication.app, 0))
    }
}