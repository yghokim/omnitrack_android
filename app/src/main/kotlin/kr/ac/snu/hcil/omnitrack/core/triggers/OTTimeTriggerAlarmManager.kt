package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.LoggingDbHelper
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

        private fun makeIntent(context: Context, user: OTUser, triggerTime: Long, alarmId: Int): PendingIntent {
            val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
            intent.action = OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, user.objectId)
            intent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId)
            intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)
            return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
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

    fun storeTableToPreferences(): Unit {
        /*
        val editor = preferences.edit()

        //reservation table
        editor.putBoolean(RESERVATION_TABLE_STORED, true)
        editor.putInt(RESERVATION_TABLE_MEMBER_THRESHOLD, reservationTable.thresholdMillis)
        editor.putStringSet(RESERVATION_TABLE_MEMBER_KEYS, reservationTable.timeKeys.map { it.toString() }.toMutableSet())


        for (entry in reservationTable) {
            editor.putStringSet(entry.key.toString(), entry.value)
        }

        //trigger table

        editor.putBoolean(TRIGGER_TABLE_STORED, true)
        editor.putStringSet(TRIGGER_TABLE_KEYS, triggerTable.keys)

        for (entry in triggerTable) {
            editor.putLong(entry.key, entry.value)
        }

        //id table
        editor.putBoolean(ID_TABLE_STORED, true)
        val ids = idTable.ids
        editor.putStringSet(ID_TABLE_IDS, ids.map { it.toString() }.toSet())

        for (id in ids) {
            editor.putLong("id_table_key_" + id.toString(), idTable.getKeyFromId(id)!!)
        }

        editor.apply()
        */
    }

    //==========================================================

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