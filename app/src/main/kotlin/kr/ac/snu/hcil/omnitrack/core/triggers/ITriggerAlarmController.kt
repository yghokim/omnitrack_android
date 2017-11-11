package kr.ac.snu.hcil.omnitrack.core.triggers

import io.reactivex.Completable

/**
 * Created by younghokim on 2017. 11. 11..
 */
interface ITriggerAlarmController {
    fun onAlarmFired(systemAlarmId: Int): Completable
}