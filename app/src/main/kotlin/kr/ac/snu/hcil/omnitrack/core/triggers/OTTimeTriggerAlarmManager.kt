package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.receivers.OTSystemReceiver
import kr.ac.snu.hcil.omnitrack.utils.FillingIntegerIdReservationTable
import kr.ac.snu.hcil.omnitrack.utils.TimeKeyValueSetTable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by younghokim on 16. 8. 29..
 */
object OTTimeTriggerAlarmManager {
    const val PREFERENCE_NAME = "TimeTriggerReservationTable"

    const val RESERVATION_TABLE_STORED = "reservationTableStored"
    const val RESERVATION_TABLE_MEMBER_THRESHOLD = "thresholdMillis"
    const val RESERVATION_TABLE_MEMBER_KEYS = "timestampList"

    const val TRIGGER_TABLE_STORED = "triggerTableStored"
    const val TRIGGER_TABLE_KEYS = "triggerTableKeys"

    const val ID_TABLE_STORED = "idTableStored"
    const val ID_TABLE_IDS = "idTableIds"

    const val INTENT_EXTRA_TRIGGER_TIME = "triggerTime"
    const val INTENT_EXTRA_ALARM_ID = "alarmId"


    private val reservationTable: TimeKeyValueSetTable<String> by lazy {
        if (preferences.getBoolean(RESERVATION_TABLE_STORED, false)) {
            val threshold = preferences.getInt(RESERVATION_TABLE_MEMBER_THRESHOLD, 500)
            val map = java.util.TreeMap<Long, MutableSet<String>>()
            val keys = preferences.getStringSet(RESERVATION_TABLE_MEMBER_KEYS, null)
            for (key in keys) {

                map[key.toLong()] = preferences.getStringSet(key, null)!!
            }

            TimeKeyValueSetTable<String>(threshold, map)
        } else {

            TimeKeyValueSetTable<String>(500)
        }
    }

    private val triggerTable: ConcurrentHashMap<String, Long> by lazy {
        if (preferences.getBoolean(TRIGGER_TABLE_STORED, false)) {
            val keys = preferences.getStringSet(TRIGGER_TABLE_KEYS, null)

            val map = ConcurrentHashMap<String, Long>(keys.size)
            for (key in keys) {
                map[key] = preferences.getLong(key, -1)
            }

            map
        } else {
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

            map
        }
    }

    private val idTable: FillingIntegerIdReservationTable<Long> by lazy {
        if (preferences.getBoolean(ID_TABLE_STORED, false)) {
            val ids = preferences.getStringSet(ID_TABLE_IDS, null).map { it.toInt() }
            val list = ArrayList<Pair<Int, Long>>()
            for (id in ids) {
                list.add(Pair<Int, Long>(id, preferences.getLong("id_table_key_" + id.toString(), -1)))
            }

            FillingIntegerIdReservationTable(list)
        } else {
            FillingIntegerIdReservationTable<Long>()
        }
    }


    private val preferences by lazy {
        OTApplication.app.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    private val alarmManager by lazy { OTApplication.app.getSystemService(Context.ALARM_SERVICE) as AlarmManager }


    init {

    }

    fun storeTableToPreferences(): Unit {
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
    }

    //==========================================================

    fun reserveAlarm(trigger: OTTrigger, alarmTime: Long) {
        val result = reservationTable.appendAndCheckIsNewKey(alarmTime, trigger.objectId, null)

        triggerTable[trigger.objectId] = result.first

        if (result.second == true) {
            //new alarm
            val alarmId = idTable[result.first]
            println("new alarm is needed for timestamp ${result.first}. alarmId is $alarmId")

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, result.first, makeIntent(OTApplication.app, result.first, alarmId))
        } else {
            println("System alarm is already registered at ${result.first}.")
        }
    }

    fun cancelTrigger(trigger: OTTrigger) {
        println(triggerTable)
        val reservedTime = triggerTable[trigger.objectId]
        if (reservedTime != null) {
            if (reservationTable.removeValueAndCheckIsTimestampEmpty(reservedTime, trigger.objectId)) {
                val alarmId = idTable[reservedTime]
                alarmManager.cancel(makeIntent(OTApplication.app, reservedTime, alarmId))
                idTable.removeKey(reservedTime)
                println("all triggers at $reservedTime was canceled. System alarm is canceled - ${alarmId}.")
            } else {
                println("only this trigger was excluded at $reservedTime. There are still ${reservationTable[reservedTime]?.size ?: 0} triggers reserved at that time. System alarm persists.")
            }

            triggerTable.remove(trigger.objectId)
        }
    }


    fun notifyAlarmFired(alarmId: Int, intentTriggerTime: Long, reallyFiredAt: Long) {
        println("alarm fired - id: $alarmId, delayed: ${reallyFiredAt - intentTriggerTime}")

        //validation
        if (idTable.getKeyFromId(alarmId) == intentTriggerTime) {
            //Toast.makeText(OTApplication.app, "Alarm fired: ${reservationTable[intentTriggerTime]?.size ?: 0} triggers are reserved for this alarm.", Toast.LENGTH_SHORT).show()

            println("${reservationTable[intentTriggerTime]?.size ?: 0} triggers are reserved for this alarm.")

            val reservedTriggers = reservationTable[intentTriggerTime]
            reservationTable.clearTimestamp(timestamp = intentTriggerTime)
            idTable.removeKey(intentTriggerTime)

            if (reservedTriggers != null) {
                for (triggerId in reservedTriggers) {
                    triggerTable.remove(triggerId)
                    val trigger = OTApplication.app.triggerManager.getTriggerWithId(triggerId)
                    trigger?.fire(intentTriggerTime)
                }
            }
        }

    }


    private fun makeIntent(context: Context, triggerTime: Long, alarmId: Int): PendingIntent {
        val intent = Intent(context, OTSystemReceiver::class.java)
        intent.action = OTApplication.BROADCAST_ACTION_TIME_TRIGGER_ALARM
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_USER, OTApplication.app.currentUser.objectId)
        intent.putExtra(INTENT_EXTRA_ALARM_ID, alarmId)
        intent.putExtra(INTENT_EXTRA_TRIGGER_TIME, triggerTime)
        return PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

}