package kr.ac.snu.hcil.omnitrack.core.triggers

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.utils.Nullable

/**
 * Created by younghokim on 2017. 11. 11..
 */
interface ITriggerAlarmController {
    fun onAlarmFired(systemAlarmId: Int): Completable

    fun registerTriggerAlarm(pivot: Long?, trigger: OTTriggerDAO)

    fun cancelTrigger(trigger: OTTriggerDAO)

    fun activateOnSystem()

    fun getNearestAlarmTime(triggerId: String, now: Long): Single<Nullable<Long>>

    fun makeNextAlarmTimeObservable(triggerId: String): Flowable<Nullable<Long>>
}