package kr.ac.snu.hcil.omnitrack.core.triggers

import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 11. 11..
 */
interface ITriggerAlarmController {
    fun onAlarmFired(systemAlarmId: Int): Completable

    fun registerTriggerAlarm(pivot: Long?, trigger: OTTriggerDAO)

    fun cancelTrigger(trigger: OTTriggerDAO)

    fun activateOnSystem()
}