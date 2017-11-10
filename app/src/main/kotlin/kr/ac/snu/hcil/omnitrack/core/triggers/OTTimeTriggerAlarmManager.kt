package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.database.AlarmInfo
import kr.ac.snu.hcil.omnitrack.core.triggers.database.AlarmInstance
import kr.ac.snu.hcil.omnitrack.core.triggers.database.TriggerSchedule
import kr.ac.snu.hcil.omnitrack.core.triggers.database.TriggerSchedulePOJO
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.utils.time.DesignatedTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.IntervalTimeScheduleCalculator
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper

/**
 * Created by younghokim on 16. 8. 29..
 */
class OTTimeTriggerAlarmManager {
    companion object {

        const val TAG = "TimeTriggerAlarmManager"

        const val PREFERENCE_NAME = "TimeTriggerReservationTable"

        const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"
        const val INTENT_EXTRA_ALARM_ID = "alarmId"

        const val DB_ALARM_SCHEDULE_UNIT_FILENAME = "alarmSchedules.db"

        private fun makeIntent(context: Context, userId: String, triggerTime: Long, alarmId: Int): PendingIntent {
            val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
            intent.action = OTApp.BROADCAST_ACTION_TIME_TRIGGER_ALARM
            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, userId)
            intent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId)
            intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)
            return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        }

        private fun findNearestAlarmInstance(alarmTime: Long, realm: Realm): AlarmInstance? {
            return realm.where(AlarmInstance::class.java).equalTo("reservedAlarmTime", TimeHelper.roundToSeconds(alarmTime)).findFirst()
        }
    }

    private val alarmManager by lazy { OTApp.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }


    private val realmDbConfiguration: RealmConfiguration by lazy {
        RealmConfiguration.Builder().name(DB_ALARM_SCHEDULE_UNIT_FILENAME).deleteRealmIfMigrationNeeded().build()
    }

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
        unManagedAlarms.forEach { alarm ->
            if (alarm.reservedAlarmTime < now) {
                //failed alarm.
                alarm.skipped = true
                alarm.triggerSchedules.load()
                alarm.triggerSchedules.forEach {
                    it.skipped = true

                    if (it.oneShot) {
                        //one-shot trigger was failed
                    } else {
                        //repeated trigger was failed.
                    }
                }
            } else {
                registerSystemAlarm(alarm.reservedAlarmTime, alarm.userId ?: "", alarm.alarmId)
            }
        }
        realm.commitTransaction()
        realm.close()
    }

    fun reserveAlarm(trigger: OTTriggerDAO, alarmTime: Long, oneShot: Boolean): AlarmInfo {
        Realm.getInstance(realmDbConfiguration).use { realm ->
            realm.beginTransaction()
            val schedule = TriggerSchedule().apply {
                this.triggerId = trigger.objectId!!
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
                /*
                * s: false, f: false
                *
                * */
                val newAlarmId = (realm.where(AlarmInstance::class.java)
                        .equalTo(AlarmInstance.FIELD_SKIPPED, false)
                        .equalTo(AlarmInstance.FIELD_FIRED, false)
                        .max("alarmId") ?: 0).toInt() + 1
                val newAlarmDbId = (realm.where(AlarmInstance::class.java).max("id") ?: 0).toLong() + 1
                val alarmInstance = realm.createObject(AlarmInstance::class.java, newAlarmDbId)
                alarmInstance.alarmId = newAlarmId

                val alarmTimeToReserve = TimeHelper.roundToSeconds(alarmTime)

                alarmInstance.reservedAlarmTime = alarmTimeToReserve
                schedule.parentAlarmId = alarmInstance.id
                alarmInstance.triggerSchedules.add(schedule)
                alarmInstance.userId = trigger.userId

                realm.insertOrUpdate(alarmInstance)
                realm.commitTransaction()

                registerSystemAlarm(alarmTimeToReserve, trigger.userId!!, newAlarmId)

                return alarmInstance.getInfo()
            }
        }
    }

    private fun registerSystemAlarm(alarmTimeToReserve: Long, userId: String, alarmId: Int) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApp.instance, userId, alarmTimeToReserve, alarmId))
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeToReserve, makeIntent(OTApp.instance, userId, alarmTimeToReserve, alarmId))
        }
    }

    fun cancelTrigger(trigger: OTTriggerDAO) {
        val realm = Realm.getInstance(realmDbConfiguration)

        val pendingTriggerSchedules = realm.where(TriggerSchedule::class.java)
                .equalTo(TriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                .equalTo(TriggerSchedule.FIELD_FIRED, false)
                .equalTo(TriggerSchedule.FIELD_SKIPPED, false)
                .findAll()

        realm.beginTransaction()
        pendingTriggerSchedules.forEach { schedule ->
            val parentAlarmId = schedule.parentAlarmId
            if (parentAlarmId != null) {
                val parentAlarm = realm.where(AlarmInstance::class.java).equalTo("id", parentAlarmId).findFirst()
                if (parentAlarm != null) {
                    parentAlarm.triggerSchedules.removeAll(
                            parentAlarm.triggerSchedules.filter { s -> s.triggerId == trigger.objectId }
                    )
                    if (parentAlarm.triggerSchedules.isEmpty()) {
                        //cancel system alarm.
                        println("remove system alarm")
                        val pendingIntent = makeIntent(OTApp.instance, trigger.userId!!, parentAlarm.reservedAlarmTime, parentAlarm.alarmId)
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
        Realm.getInstance(realmDbConfiguration).use { realm ->
            val nearestTime = realm.where(TriggerSchedule::class.java)
                    .equalTo(TriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                    .equalTo(TriggerSchedule.FIELD_FIRED, false)
                    .equalTo(TriggerSchedule.FIELD_SKIPPED, false)
                    .greaterThan(TriggerSchedule.FIELD_INTRINSIC_ALARM_TIME, now)
                    .min(TriggerSchedule.FIELD_INTRINSIC_ALARM_TIME)

            return nearestTime?.toLong()
        }
    }

    fun handleAlarmAndGetTriggerInfo(alarmId: Int): List<TriggerSchedulePOJO>? {
        Realm.getInstance(realmDbConfiguration).use { realm ->
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


    fun getNextAlarmTime(pivot: Long, condition: OTTimeTriggerCondition): Long? {
        val now = System.currentTimeMillis()

        val limitExclusive = if (condition.isRepeated && condition.endAt != null) {
            condition.endAt!!
        } else Long.MAX_VALUE

        val nextTimeCalculator = when (condition.timeConditionType) {
            OTTimeTriggerCondition.TIME_CONDITION_ALARM -> {

                val calculator = DesignatedTimeScheduleCalculator().setAlarmTime(condition.alarmTimeHour.toInt(), condition.alarmTimeMinute.toInt(), 0)
                if (condition.isRepeated) {
                    calculator.setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                            .setEndAt(limitExclusive)
                }

                calculator
            }
            OTTimeTriggerCondition.TIME_CONDITION_INTERVAL -> {
                val intervalMillis = condition.intervalSeconds * 1000

                val calculator = IntervalTimeScheduleCalculator().setInterval(intervalMillis.toLong())

                if (condition.isRepeated) {
                    calculator
                            .setAvailableDaysOfWeekFlag(condition.dayOfWeekFlags.toInt())
                            .setEndAt(limitExclusive)

                    if (condition.intervalIsHourRangeUsed) {
                        calculator.setHourBoundingRange(condition.intervalHourRangeStart.toInt(), condition.intervalHourRangeEnd.toInt())
                    }
                } else {
                    calculator.setNoLimit()
                }

                calculator
            }
            else -> throw Exception("Unsupported Time Config Type")
        }

        return nextTimeCalculator.calculateNext(pivot, now)
    }

}