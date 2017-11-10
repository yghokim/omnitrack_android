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

    }

    fun handleTriggerOff(managedTrigger: OTTriggerDAO) {

    }

    fun detachFromSystem(managedTrigger: OTTriggerDAO) {

    }
}