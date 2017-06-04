package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.triggers.database.AlarmInfo
import kr.ac.snu.hcil.omnitrack.core.triggers.database.AlarmInstance
import kr.ac.snu.hcil.omnitrack.core.triggers.database.TriggerSchedule
import kr.ac.snu.hcil.omnitrack.core.triggers.database.TriggerSchedulePOJO
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper

/**
 * Created by younghokim on 16. 8. 29..
 */
class OTTimeTriggerAlarmManager() {
    companion object {

        const val TAG = "TimeTriggerAlarmManager"

        const val PREFERENCE_NAME = "TimeTriggerReservationTable"

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"
        const val INTENT_EXTRA_ALARM_ID = "alarmId"

        const val DB_ALARM_SCHEDULE_UNIT_FILENAME = "alarmSchedules.db"

        val realmDbConfiguration: RealmConfiguration by lazy {
            RealmConfiguration.Builder().name(DB_ALARM_SCHEDULE_UNIT_FILENAME).deleteRealmIfMigrationNeeded().build()
        }

        private fun makeIntent(context: Context, user: OTUser, triggerTime: Long, alarmId: Int): PendingIntent {
            val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
            intent.action = OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, user.objectId)
            intent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId)
            intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)
            return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        private fun findNearestAlarmInstance(alarmTime: Long, realm: Realm): AlarmInstance? {
            return realm.where(AlarmInstance::class.java).equalTo("reservedAlarmTime", TimeHelper.roundToSeconds(alarmTime)).findFirst()
        }
    }

    private val alarmManager by lazy { OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    init {
    }


    //==========================================================

    fun activateOnSystem() {
        val realm = Realm.getInstance(realmDbConfiguration)

        val unManagedAlarms = realm.where(AlarmInstance::class.java)
                .equalTo(AlarmInstance.FIELD_FIRED, false)
                .equalTo(AlarmInstance.FIELD_SKIPPED, false).findAll()

        val now = System.currentTimeMillis() + 1000

        realm.beginTransaction()
        unManagedAlarms.forEach {
            alarm ->
            if (alarm.reservedAlarmTime < now) {
                //failed alarm.
                alarm.skipped = true
                alarm.triggerSchedules.load()
                alarm.triggerSchedules.forEach {
                    it.skipped = true
                }
            } else {

            }
        }
        realm.commitTransaction()
        realm.close()
    }

    fun reserveAlarm(trigger: OTTrigger, alarmTime: Long, oneShot: Boolean): AlarmInfo {
        Realm.getInstance(realmDbConfiguration).use {
            realm ->
            realm.beginTransaction()
            val schedule = TriggerSchedule().apply {
                this.triggerId = trigger.objectId
                this.intrinsicAlarmTime = alarmTime
                this.oneShot = oneShot
            }

            val appendableAlarm = findNearestAlarmInstance(alarmTime, realm)
            if (appendableAlarm != null) {
                schedule.parentAlarmId = appendableAlarm.id
                appendableAlarm.triggerSchedules.add(schedule)
                realm.insertOrUpdate(appendableAlarm)
                realm.commitTransaction()
                return appendableAlarm.getInfo()
            } else {
                //make new alarm
                val newAlarmId = (realm.where(AlarmInstance::class.java).max("alarmId") ?: 0).toInt() + 1
                val newAlarmDbId = (realm.where(AlarmInstance::class.java).max("id") ?: 0).toLong() + 1
                val alarmInstance = realm.createObject(AlarmInstance::class.java, newAlarmDbId)
                alarmInstance.alarmId = newAlarmId

                val alarmTimeToReserve = TimeHelper.roundToSeconds(alarmTime)

                alarmInstance.reservedAlarmTime = alarmTimeToReserve
                schedule.parentAlarmId = alarmInstance.id
                alarmInstance.triggerSchedules.add(schedule)

                realm.insertOrUpdate(alarmInstance)
                realm.commitTransaction()

                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApplication.app, trigger.user, alarmTime, newAlarmId))
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApplication.app, trigger.user, alarmTime, newAlarmId))
                }

                return alarmInstance.getInfo()
            }
        }
    }

    fun cancelTrigger(trigger: OTTrigger) {
        val realm = Realm.getInstance(realmDbConfiguration)

        val pendingTriggerSchedules = realm.where(TriggerSchedule::class.java)
                .equalTo(TriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                .equalTo(TriggerSchedule.FIELD_FIRED, false)
                .equalTo(TriggerSchedule.FIELD_SKIPPED, false)
                .findAll()

        OTApplication

        realm.beginTransaction()
        pendingTriggerSchedules.forEach {
            schedule ->
            val parentAlarmId = schedule.parentAlarmId
            if (parentAlarmId != null) {
                val parentAlarm = realm.where(AlarmInstance::class.java).equalTo("id", parentAlarmId).findFirst()
                if (parentAlarm != null) {
                    parentAlarm.triggerSchedules.removeIf { s -> s.triggerId == trigger.objectId }
                    if (parentAlarm.triggerSchedules.isEmpty()) {
                        //cancel system alarm.
                        println("remove system alarm")
                        val pendingIntent = makeIntent(OTApplication.app, trigger.user, parentAlarm.reservedAlarmTime, parentAlarm.alarmId)
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                        parentAlarm.deleteFromRealm()
                    }
                } else {
                    println("parent alarm is null.")
                }
            } else {
                println("parent alarm id is null.")
            }
        }
        pendingTriggerSchedules.deleteAllFromRealm()
        realm.commitTransaction()

        realm.close()
    }

    fun getNearestAlarmTime(trigger: OTTimeTrigger, now: Long): Long? {
        Realm.getInstance(realmDbConfiguration).use {
            realm ->
            val nearestTime = realm.where(TriggerSchedule::class.java)
                    .equalTo(TriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                    .equalTo(TriggerSchedule.FIELD_FIRED, false)
                    .equalTo(TriggerSchedule.FIELD_SKIPPED, false)
                    .greaterThan(TriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, now)
                    .min(TriggerSchedule.FIELD_INTRINSIC_ALARM_TIME)

            return nearestTime?.toLong()
        }
    }

    fun handleAlarmAndGetTriggerInfo(user: OTUser, alarmId: Int, intentTriggerTime: Long, reallyFiredAt: Long): List<TriggerSchedulePOJO>? {
        Realm.getInstance(realmDbConfiguration).use {
            realm ->
            println("find alarm instance with id: ${alarmId}")
            val alarmInstance = realm.where(AlarmInstance::class.java)
                    .equalTo(AlarmInstance.FIELD_ALARM_ID, alarmId)
                    .equalTo(AlarmInstance.FIELD_FIRED, false)
                    .equalTo(AlarmInstance.FIELD_SKIPPED, false)
                    .findFirst()

            if (alarmInstance != null) {
                realm.beginTransaction()


                alarmInstance.triggerSchedules.forEach { schedule ->
                    schedule.fired = true
                    schedule.skipped = false
                }

                alarmInstance.fired = true
                alarmInstance.skipped = false

                realm.commitTransaction()

                println("trigger schedules: ${alarmInstance.triggerSchedules.size}")
                return alarmInstance.triggerSchedules.map { it.getPOJO() }
            } else return null
        }
    }
}