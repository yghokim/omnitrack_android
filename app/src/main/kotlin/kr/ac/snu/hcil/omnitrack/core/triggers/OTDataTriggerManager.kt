package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.gcm.GcmNetworkManager
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.services.DataTriggerCheckService
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 9. 5
 */
object OTDataTriggerManager {

    const val CHECK_PERIOD = 30L // 1 minute
    const val ALARM_ID: Int = 9000000

    const val PREFERENCE_LAST_CONDITION_TABLE = "EventTriggerLastCondition"
    const val PREFERENCE_ENROLLED_IDS = "EventTriggerEnrolledIds"
    const val PREFERENCE_TRIGGER_ID_PREFIX = "ET_"

    //private var batch: MeasureBatchCheckTask? = null

    private val preferences: SharedPreferences by lazy {
        OTApp.instance.getSharedPreferences(PREFERENCE_LAST_CONDITION_TABLE, Context.MODE_PRIVATE)
    }

    private val alarmManager by lazy { OTApp.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    private fun prefKey(trigger: OTDataTrigger): String {
        return PREFERENCE_TRIGGER_ID_PREFIX + trigger.objectId
    }

    /*
    private fun makeIntent(context: Context): PendingIntent {
        val intent = Intent(context, OTSystemReceiver::class.java)
        intent.action = OTApp.BROADCAST_ACTION_EVENT_TRIGGER_CHECK_ALARM
        intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_USER, OTApp.instance.currentUser.objectId)
        return PendingIntent.getBroadcast(context, ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }*/

    private val gcmManager: GcmNetworkManager by lazy {
        GcmNetworkManager.getInstance(OTApp.instance)
    }

    fun onEventTriggerOn(trigger: OTDataTrigger) {

        println("event trigger is registered on system.")

        val ids = preferences.getStringSet(PREFERENCE_ENROLLED_IDS, HashSet<String>())
        println(ids)
        if (!ids.contains(trigger.objectId)) {
            ids.add(trigger.objectId)
            preferences.edit().putBoolean(prefKey(trigger), false).putStringSet(PREFERENCE_ENROLLED_IDS, ids).apply()
        }
        gcmManager.schedule(DataTriggerCheckService.makeTask(CHECK_PERIOD))
        //alarmManager.setsetInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), CHECK_PERIOD, makeIntent(OTApp.instance))
    }

    fun onEventTriggerOff(trigger: OTDataTrigger) {

        println("event trigger is unregistered on system.")

        if (preferences.contains(prefKey(trigger))) {
            val ids = preferences.getStringSet(PREFERENCE_ENROLLED_IDS, HashSet<String>())
            ids.remove(trigger.objectId)
            preferences.edit().remove(prefKey(trigger)).putStringSet(PREFERENCE_ENROLLED_IDS, ids).apply()

            if (ids.size == 0) {
                println("No event trigger is on. Cancel alarm.")
                //alarmManager.cancel(makeIntent(OTApp.instance))


                gcmManager.cancelAllTasks(DataTriggerCheckService::class.java)
            }
        }
    }

    fun checkMeasures(@Suppress("UNUSED_PARAMETER") context: Context) {
        println("checking measures for event triggers...")

        //val triggersToCheck = OTApp.instance.currentUser.triggerManager.getFilteredTriggers { it is OTDataTrigger && it.isOn == true }.map { it as OTDataTrigger }.toTypedArray()
/*
        batch?.cancel(true)

        batch = MeasureBatchCheckTask {
            batch = null
        }
        batch?.execute(*triggersToCheck)*/
    }
/*
    private class MeasureBatchCheckTask(val finishedHandler: (BooleanArray) -> Unit) : AsyncTask<OTDataTrigger, OTDataTrigger, BooleanArray>() {
        override fun onProgressUpdate(vararg values: OTDataTrigger) {
            super.onProgressUpdate(*values)
            println("Fire Event trigger - ${values[0].measure?.factoryCode}, ${values[0].conditioner}")
            values[0].fire(System.currentTimeMillis())
        }

        override fun onPostExecute(result: BooleanArray) {
            super.onPostExecute(result)
            println("finished batch measure check.")
            finishedHandler.invoke(result)
        }

        override fun doInBackground(vararg triggers: OTDataTrigger): BooleanArray {
            val measureResults = BooleanArray(triggers.size)
            var left = triggers.size
            for(trigger in triggers.withIndex())
            {
                val measure = trigger.value.measure
                measure?.requestLatestValueAsync {
                    value->
                    println("event trigger checked value : $value")
                    if(value!=null)
                    {
                        measureResults[trigger.index] = trigger.value.conditioner?.validate(value) ?: false
                    }
                    else{
                        measureResults[trigger.index] = false
                    }
                    left--
                    } ?: left--
            }

            while(left>0){}

            //finished
            for(passed in measureResults.withIndex())
            {
                if(passed.value == true)
                {
                    if(preferences.getBoolean(prefKey(triggers[passed.index]), false) == false)
                    {
                        publishProgress( triggers[passed.index])

                    }
                }
            }

            val editor = preferences.edit()

            for(passed in measureResults.withIndex())
            {
                editor.putBoolean(prefKey(triggers[passed.index]), passed.value)
            }

            editor.apply()

            return measureResults
        }

    }*/
}