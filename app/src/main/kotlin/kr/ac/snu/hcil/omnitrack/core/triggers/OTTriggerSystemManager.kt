package kr.ac.snu.hcil.omnitrack.core.triggers

import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 9..
 */
@Singleton
class OTTriggerSystemManager(
        val timeTriggerAlarmManager: Lazy<OTTimeTriggerAlarmManager>
) {

    fun onSystemRebooted() {
        timeTriggerAlarmManager.get().activateOnSystem()
    }

    fun handleTriggerOn(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOn: ${managedTrigger.objectId}")
    }

    fun handleTriggerOff(managedTrigger: OTTriggerDAO) {
        println("TriggerSystemManager: handleTriggerOff: ${managedTrigger.objectId}")

    }

    fun tryCheckInToSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckInToSystem: ${managedTrigger.objectId}")
        return false
    }

    fun tryCheckOutFromSystem(managedTrigger: OTTriggerDAO): Boolean {
        println("TriggerSystemManager: tryCheckOutFromSystem: ${managedTrigger.objectId}")
        return false
    }
}