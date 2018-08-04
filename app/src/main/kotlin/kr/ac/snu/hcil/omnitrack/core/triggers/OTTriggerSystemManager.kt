package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter

/**
 * Created by younghokim on 2017. 11. 9..
 */
class OTTriggerSystemManager(
        val triggerAlarmManager: Lazy<ITriggerAlarmController>,
        val realmProvider: Factory<Realm>,
        val context: Context,
        val configuredContext: ConfiguredContext
) {
    private val settingsPrompter: OTExternalSettingsPrompter by lazy {
        OTExternalSettingsPrompter(context)
    }

    fun onSystemRebooted() {
        triggerAlarmManager.get().activateOnSystem()
    }

    fun handleTriggerOn(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOn: ${managedTrigger.objectId}")
        when (managedTrigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                triggerAlarmManager.get().registerTriggerAlarm(System.currentTimeMillis(), managedTrigger)
            }
        }
    }

    fun handleTriggerOff(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOff: ${managedTrigger.objectId}")
        when (managedTrigger.conditionType) {
            OTTriggerDAO.CONDITION_TYPE_TIME -> {
                triggerAlarmManager.get().cancelTrigger(managedTrigger)
            }
        }
    }


    fun tryCheckInToSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckInToSystem: ${managedTrigger.objectId}")
        if (BuildConfig.DISABLE_EXTERNAL_ENTITIES == false || managedTrigger.experimentIdInFlags == BuildConfig.DEFAULT_EXPERIMENT_ID) {
            if (managedTrigger.isOn) {

                when (managedTrigger.conditionType) {
                    OTTriggerDAO.CONDITION_TYPE_TIME -> {
                        triggerAlarmManager.get().continueTriggerInChainIfPossible(managedTrigger)
                        if (managedTrigger.isOn) {
                            if (!settingsPrompter.isBatteryOptimizationWhiteListed()) {
                                settingsPrompter.askUserBatterOptimizationWhitelist()
                            }
                        }
                    }
                }
            } else {
                handleTriggerOff(managedTrigger)
            }
        }
        return false
    }

    fun tryCheckOutFromSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckOutFromSystem: ${managedTrigger.objectId}")
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
        val realm = realmProvider.get()

        val triggers = realm.where(OTTriggerDAO::class.java)
                .equalTo(BackendDbManager.FIELD_USER_ID, userId)
                .equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
                .equalTo("isOn", true)
                .findAll()

        triggers.forEach { trigger ->
            if (trigger.liveTrackerCount > 0) {
                if (BuildConfig.DISABLE_EXTERNAL_ENTITIES && trigger.experimentIdInFlags != BuildConfig.DEFAULT_EXPERIMENT_ID) {
                    tryCheckOutFromSystem(trigger)
                } else tryCheckInToSystem(trigger)
            }
        }
        val numTriggers = triggers.size
        realm.close()
        return numTriggers
    }
}