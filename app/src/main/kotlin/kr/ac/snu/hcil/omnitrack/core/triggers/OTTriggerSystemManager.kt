package kr.ac.snu.hcil.omnitrack.core.triggers

import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
class OTTriggerSystemManager(
        val triggerAlarmManager: Lazy<ITriggerAlarmController>


) {

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
                    return triggerAlarmManager.get().continueTriggerInChainIfPossible(managedTrigger)
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