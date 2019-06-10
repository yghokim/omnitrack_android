package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter

/**
 * Created by younghokim on 2017. 11. 9..
 */
class OTTriggerSystemManager(
        val triggerAlarmManager: Lazy<ITriggerAlarmController>,
        val dataDrivenTriggerManager: Lazy<OTDataDrivenTriggerManager>,
        val realmProvider: Factory<Realm>,
        val context: Context
) {
    private val settingsPrompter: OTExternalSettingsPrompter by lazy {
        OTExternalSettingsPrompter(context)
    }

    private val reminderCommands: OTReminderCommands by lazy {
        OTReminderCommands(context)
    }

    fun onSystemRebooted() {
        triggerAlarmManager.get().activateOnSystem()
        dataDrivenTriggerManager.get().activateOnSystem()
    }

    fun handleTriggerOn(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOn: ${managedTrigger._id}")
        when (managedTrigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                triggerAlarmManager.get().registerTriggerAlarm(System.currentTimeMillis(), managedTrigger)
            }
            OTTriggerDAO.CONDITION_TYPE_DATA -> {
                dataDrivenTriggerManager.get().registerTrigger(managedTrigger)
            }
        }
    }

    fun handleTriggerOff(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOff: ${managedTrigger._id}")
        when (managedTrigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                triggerAlarmManager.get().cancelTrigger(managedTrigger)
            }
            OTTriggerDAO.CONDITION_TYPE_DATA -> {
                dataDrivenTriggerManager.get().unregisterTrigger(managedTrigger)
            }
        }

        //turn off reminder
        when (managedTrigger.actionType) {
            OTTriggerDAO.ACTION_TYPE_REMIND -> {
                val realm = realmProvider.get()
                reminderCommands.dismissAllReminders(realm, managedTrigger)
                realm.close()
            }
        }
    }


    fun tryCheckInToSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckInToSystem: ${managedTrigger._id}")
        if (BuildConfig.DEFAULT_EXPERIMENT_ID == null || managedTrigger.experimentIdInFlags == BuildConfig.DEFAULT_EXPERIMENT_ID) {
            if (managedTrigger.isOn) {
                when (managedTrigger.conditionType) {
                    OTTriggerDAO.CONDITION_TYPE_TIME -> {
                        triggerAlarmManager.get().continueTriggerInChainIfPossible(managedTrigger)
                        if (!settingsPrompter.isBatteryOptimizationWhiteListed()) {
                            settingsPrompter.askUserBatterOptimizationWhitelist()
                        }
                    }
                    OTTriggerDAO.CONDITION_TYPE_DATA -> {
                        dataDrivenTriggerManager.get().registerTrigger(managedTrigger)
                    }
                }
            } else {
                handleTriggerOff(managedTrigger)
            }
        }
        return false
    }

    fun tryCheckOutFromSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckOutFromSystem: ${managedTrigger._id}")
        handleTriggerOff(managedTrigger)
        return false
    }

    fun refreshReservedAlarms() {
        triggerAlarmManager.get().rearrangeSystemAlarms()
        val realm = realmProvider.get()
        val commands = OTReminderCommands(context)
        commands.rearrangeAutoExpiryAlarms(realm)
        realm.close()
    }

    fun checkOutAllFromSystem(userId: String): Int {
        val realm = realmProvider.get()

        val triggers = realm.where(OTTriggerDAO::class.java)
                .equalTo(BackendDbManager.FIELD_USER_ID, userId)
                .equalTo("isOn", true)
                .findAll()

        triggers.forEach { trigger ->
            tryCheckOutFromSystem(trigger)
        }
        val numTriggers = triggers.size
        realm.close()
        return numTriggers
    }

    fun checkInAllToSystem(userId: String): Int {
        OTApp.logger.writeSystemLog("Checkin all the triggers in the system.", "TriggerSystemManager")
        val realm = realmProvider.get()

        val triggers = realm.where(OTTriggerDAO::class.java)
                .equalTo(BackendDbManager.FIELD_USER_ID, userId)
                .equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
                .equalTo("isOn", true)
                .findAll()

        dataDrivenTriggerManager.get().setSuspendReadjustWorker(true)
        triggers.forEach { trigger ->
            if (trigger.liveTrackerCount > 0) {
                if (BuildConfig.DEFAULT_EXPERIMENT_ID != null && trigger.experimentIdInFlags != BuildConfig.DEFAULT_EXPERIMENT_ID) {
                    tryCheckOutFromSystem(trigger)
                } else tryCheckInToSystem(trigger)
            }
        }
        val numTriggers = triggers.size
        dataDrivenTriggerManager.get().setSuspendReadjustWorker(false)
        dataDrivenTriggerManager.get().reAdjustWorker(realm)
        realm.close()
        return numTriggers
    }
}