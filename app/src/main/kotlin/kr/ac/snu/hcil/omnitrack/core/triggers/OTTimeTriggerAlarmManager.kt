package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.realm.Realm
import io.realm.RealmConfiguration
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.database.AlarmInstance
import kr.ac.snu.hcil.omnitrack.core.triggers.database.TriggerSchedule
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import kr.ac.snu.hcil.omnitrack.utils.time.TimeKeyValueSetTable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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

        const val ALARM_TOLERANCE = 300

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
            return realm.where(AlarmInstance::class.java).between("reservedAlarmTime", alarmTime - ALARM_TOLERANCE, alarmTime + ALARM_TOLERANCE).findFirst()
        }
    }

    private val reservationTable: TimeKeyValueSetTable<String>

    private val triggerTable: ConcurrentHashMap<String, Long>

    private val idTable: FillingIntegerIdReservationTable<Long>


    private val preferences by lazy {
        OTApplication.app.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    private val alarmManager by lazy { OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager }


    init {
        reservationTable = /*if (preferences.getBoolean(RESERVATION_TABLE_STORED, false)) {
            val threshold = preferences.getInt(RESERVATION_TABLE_MEMBER_THRESHOLD, 500)
            val map = java.util.TreeMap<Long, MutableSet<String>>()
            val keys = preferences.getStringSet(RESERVATION_TABLE_MEMBER_KEYS, null)
            for (key in keys) {

                map[key.toLong()] = preferences.getStringSet(key, null)!!
            }

            TimeKeyValueSetTable<String>(threshold, map)
        } else {
*/
            TimeKeyValueSetTable<String>(500)
        //       }

        triggerTable = /*if (preferences.getBoolean(TRIGGER_TABLE_STORED, false)) {
            val keys = preferences.getStringSet(TRIGGER_TABLE_KEYS, null)

            val map = ConcurrentHashMap<String, Long>(keys.size)
            for (key in keys) {
                map[key] = preferences.getLong(key, -1)
            }

            map
        } else {
        */
            ConcurrentHashMap<String, Long>()
            val map = ConcurrentHashMap<String, Long>()

            for (entry in reservationTable) {
                for (triggerId in entry.value) {
                    if (map[triggerId] == null) {
                        map[triggerId] = entry.key
                    } else {
                        throw Exception("trigger should be found once in reservation table.")
                    }
                }
            }
        //}

        idTable = /*if (preferences.getBoolean(ID_TABLE_STORED, false)) {
            val ids = preferences.getStringSet(ID_TABLE_IDS, null).map { it.toInt() }
            val list = ArrayList<Pair<Int, Long>>()
            for (id in ids) {
                list.add(Pair<Int, Long>(id, preferences.getLong("id_table_key_" + id.toString(), -1)))
            }

            FillingIntegerIdReservationTable(list)
        } else {*/
            FillingIntegerIdReservationTable<Long>()
        //}
    }


    //==========================================================

    fun reserveAlarm(trigger: OTTrigger, alarmTime: Long, oneShot: Boolean) {
        val realm = Realm.getInstance(realmDbConfiguration)
        realm.beginTransaction()
        val result = realm.where(TriggerSchedule::class.java).equalTo("triggerId", trigger.objectId).findAll()
        val schedule = TriggerSchedule().apply {
            this.triggerId = trigger.objectId
            this.intrinsicAlarmTime = alarmTime
            this.oneShot = oneShot
        }

        val appendableAlarm = findNearestAlarmInstance(alarmTime, realm)
        if (appendableAlarm != null) {
            appendableAlarm.triggerSchedules.add(schedule)
            schedule.parentAlarm = appendableAlarm
            realm.copyToRealm(schedule)
            realm.insertOrUpdate(appendableAlarm)

            realm.commitTransaction()
            realm.close()
        } else {
            //make new alarm
            val newAlarmId = (realm.where(AlarmInstance::class.java).max("alarmId") ?: 0).toInt() + 1
            val alarmInstance = realm.createObject(AlarmInstance::class.java)
            alarmInstance.alarmId = newAlarmId
            alarmInstance.reservedAlarmTime = alarmTime
            alarmInstance.triggerSchedules.add(schedule)
            schedule.parentAlarm = alarmInstance

            realm.copyToRealm(schedule)
            realm.insertOrUpdate(alarmInstance)

            realm.commitTransaction()
            realm.close()

            if (android.os.Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, makeIntent(OTApplication.app, trigger.user, alarmTime, newAlarmId))
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, makeIntent(OTApplication.app, trigger.user, alarmTime, newAlarmId))
            }
        }
    }

    fun cancelTrigger2(trigger: OTTrigger) {
        val realm = Realm.getInstance(realmDbConfiguration)

        val pendingTriggerSchedules = realm.where(TriggerSchedule::class.java)
                .equalTo(TriggerSchedule.FIELD_TRIGGER_ID, trigger.objectId)
                .equalTo(TriggerSchedule.FIELD_FIRED, false)
                .equalTo(TriggerSchedule.FIELD_SKIPPED, false)
                .findAll()

        realm.beginTransaction()
        pendingTriggerSchedules.forEach {
            schedule ->
            val parentAlarm = schedule.parentAlarm
            if (parentAlarm != null) {
                parentAlarm.triggerSchedules.removeIf { s -> s.triggerId == trigger.objectId }
                if (parentAlarm.triggerSchedules.isEmpty()) {
                    //cancel system alarm.
                    val pendingIntent = makeIntent(OTApplication.app, trigger.user, parentAlarm.reservedAlarmTime, parentAlarm.alarmId)
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    parentAlarm.deleteFromRealm()
                }
            }
        }
        pendingTriggerSchedules.deleteAllFromRealm()
        realm.commitTransaction()

        realm.close()
    }


    fun handleAlarmAndGetTriggerIds(user: OTUser, alarmId: Int, intentTriggerTime: Long, reallyFiredAt: Long): List<String>? {
        val realm = Realm.getInstance(realmDbConfiguration)
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
            realm.commitTransaction()

            return alarmInstance.triggerSchedules.map { it.triggerId }
        } else return null
    }

    fun reserveAlarm(trigger: OTTrigger, alarmTime: Long) {

        //check if the trigger already exists in triggerTable


        if (triggerTable[trigger.objectId] != null) {
            OTApplication.logger.writeSystemLog("Trigger already exists. reserved trigger time - ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(triggerTable[trigger.objectId]!!))}", TAG)
            val reservedTriggerTime = triggerTable[trigger.objectId]!!

            if (reservedTriggerTime >= alarmTime) {
                OTApplication.logger.writeSystemLog("check the system alarm exists.", TAG)

                val reservedAlarmId = idTable[reservedTriggerTime]

                val intent = Intent(OTApplication.app, TimeTriggerAlarmReceiver::class.java)
                intent.action = OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM

                val isIntentExists = PendingIntent.getBroadcast(OTApplication.app, reservedAlarmId, intent, PendingIntent.FLAG_NO_CREATE) != null

                if (isIntentExists) {
                    OTApplication.logger.writeSystemLog("alarm is already registered. skip this reservation.", TAG)
                    return
                } else {
                    cancelTrigger(trigger)
                }
            } else {
                cancelTrigger(trigger)
            }
        }

        val result = reservationTable.appendAndCheckIsNewKey(alarmTime, trigger.objectId, null)

        triggerTable[trigger.objectId] = result.first

        OTApplication.logger.writeSystemLog("Reserving alarm for trigger ${trigger.objectId}", TAG)


        if (result.second == true) {
            //new alarm
            val alarmId = idTable[result.first]
            println("new alarm is needed for timestamp ${result.first}. alarmId is $alarmId")

            if (android.os.Build.VERSION.SDK_INT >= 23) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, result.first, makeIntent(OTApplication.app, trigger.user, result.first, alarmId))
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, result.first, makeIntent(OTApplication.app, trigger.user, result.first, alarmId))
            }

            OTApplication.logger.writeSystemLog("Set new system alarm at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(result.first))}", TAG)
        } else {
            OTApplication.logger.writeSystemLog("System alarm is already registered at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(result.first))}", TAG)
            println("System alarm is already registered at ${result.first}.")
        }
    }

    fun cancelTrigger(trigger: OTTrigger) {
        OTApplication.logger.writeSystemLog("Canceling the alarm for trigger ${trigger.objectId}", "TimeTriggerAlarmManager")

        val reservedTime = triggerTable[trigger.objectId]
        if (reservedTime != null) {
            if (reservationTable.removeValueAndCheckIsTimestampEmpty(reservedTime, trigger.objectId)) {
                val alarmId = idTable[reservedTime]

                val pendingIntent = makeIntent(OTApplication.app, trigger.user, reservedTime, alarmId)
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()

                idTable.removeKey(reservedTime)
                println("all triggers at $reservedTime was canceled. System alarm is canceled - ${alarmId}.")
                OTApplication.logger.writeSystemLog("all triggers at ${LoggingDbHelper.TIMESTAMP_FORMAT.format(Date(reservedTime))} was canceled. System alarm is canceled - ${alarmId}.", TAG)
            } else {
                println("only this trigger was excluded at $reservedTime. There are still ${reservationTable[reservedTime]?.size ?: 0} triggers reserved at that time. System alarm persists.")
                OTApplication.logger.writeSystemLog("only this trigger was excluded at $reservedTime. There are still ${reservationTable[reservedTime]?.size ?: 0} triggers reserved at that time. System alarm persists.", TAG)
            }

            triggerTable.remove(trigger.objectId)
        } else {
            OTApplication.logger.writeSystemLog("no reserved alarm of the trigger. trigger canceling is in vain", TAG)
        }
    }

    fun notifyAlarmFiredAndGetTriggersSync(user: OTUser, alarmId: Int, intentTriggerTime: Long, reallyFiredAt: Long): List<OTTrigger>? {

        println("alarm fired - id: $alarmId, delayed: ${reallyFiredAt - intentTriggerTime}")

        OTApplication.logger.writeSystemLog("alarm fired - id: $alarmId, delayed: ${reallyFiredAt - intentTriggerTime}", OTTimeTriggerAlarmManager.TAG)

        OTApplication.logger.writeSystemLog("# of timestamps : ${reservationTable.size}, # of triggers: ${triggerTable.size}", OTTimeTriggerAlarmManager.TAG)
        //validation

        val reservedTimeOfAlarm = idTable.getKeyFromId(alarmId)

        OTApplication.logger.writeSystemLog("idTableSearchResult: $reservedTimeOfAlarm, intentTriggerTime: ${intentTriggerTime}", OTTimeTriggerAlarmManager.TAG)

        if (reservedTimeOfAlarm == intentTriggerTime) {
            //Toast.makeText(OTApplication.app, "Alarm fired: ${reservationTable[intentTriggerTime]?.size ?: 0} triggers are reserved for this alarm.", Toast.LENGTH_SHORT).show()

            println("${reservationTable[intentTriggerTime]?.size ?: 0} triggers are reserved for this alarm.")
            OTApplication.logger.writeSystemLog("${reservationTable.size} timestamps are stored", OTTimeTriggerAlarmManager.TAG)
            val reservedTriggers = reservationTable[intentTriggerTime]
            reservationTable.clearTimestamp(timestamp = intentTriggerTime)
            idTable.removeKey(intentTriggerTime)

            if (reservedTriggers != null) {
                val triggers = ArrayList<OTTrigger>()
                for (triggerId in reservedTriggers) {
                    triggerTable.remove(triggerId)
                    val trigger = user.triggerManager.getTriggerWithId(triggerId)
                    if (trigger != null)
                        triggers.add(trigger)
                }
                return triggers
            } else return null
        } else if (reservedTimeOfAlarm != null) {
            OTApplication.logger.writeSystemLog("alarm is reserved but at different time. run trigger and keep the reservation.", OTTimeTriggerAlarmManager.TAG)

            val reservedTriggers = reservationTable[reservedTimeOfAlarm]
            if (reservedTriggers != null) {
                val triggers = ArrayList<OTTrigger>()
                for (triggerId in reservedTriggers) {
                    val trigger = user.triggerManager.getTriggerWithId(triggerId)
                    if (trigger != null)
                        triggers.add(trigger)
                }
                return triggers
            } else return null
        } else return null
    }

}