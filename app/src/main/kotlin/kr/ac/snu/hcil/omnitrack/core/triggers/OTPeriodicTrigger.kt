package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.receivers.OTSystemReceiver

/**
 * Created by younghokim on 16. 7. 27..
 */
class OTPeriodicTrigger : OTTrigger {

    override val typeId: Int = TYPE_PERIODIC

    override val typeNameResourceId: Int = R.string.trigger_periodic_name
    override val descriptionResourceId: Int = R.string.trigger_periodic_desc

    var period: Long by properties
    var pivot: Long by properties

    constructor(name: String, tracker: OTTracker) : super(name, tracker)
    constructor(objectId: String?, dbId: Long?, name: String, trackerObjectId: String, isOn: Boolean, serializedProperties: String? = null) : super(objectId, dbId, name, trackerObjectId, isOn, serializedProperties)

    private fun makeIntent(context: Context, alarmId: Int): PendingIntent {
        val intent = Intent(context, OTSystemReceiver::class.java)
        intent.action = OmniTrackApplication.BROADCAST_ACTION_ALARM
        intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRIGGER, this.objectId)
        intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_USER, OmniTrackApplication.app.currentUser.objectId)

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

            val alarmManager = OmniTrackApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.set(AlarmManager.RTC, nextAlarmTime, makeIntent(OmniTrackApplication.app, 0))
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
        val alarmManager = OmniTrackApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarmManager.cancel(makeIntent(OmniTrackApplication.app, 0))
    }
}