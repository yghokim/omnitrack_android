package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.system.OTExternalSettingsPrompter
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
class OTTriggerSystemManager(
        val triggerAlarmManager: Lazy<ITriggerAlarmController>,
        val context: Context
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
        return false
    }

    fun tryCheckOutFromSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckOutFromSystem: ${managedTrigger.objectId}")
        handleTriggerOff(managedTrigger)
        return false
    }
}