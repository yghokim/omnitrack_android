package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.triggers.ITriggerAlarmController
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 11. 12..
 */
class TimeConditionViewModel(trigger: OTTriggerDAO, app: OTApp) : ATriggerConditionViewModel(trigger, OTTriggerDAO.CONDITION_TYPE_TIME) {

    private val subscriptions = CompositeDisposable()

    @Inject
    protected lateinit var triggerAlarmManager: ITriggerAlarmController

    private val nextAlarmTimeSubject = BehaviorSubject.createDefault(Nullable<Long>(null))
    val nextAlarmTime: Observable<Nullable<Long>> get() = nextAlarmTimeSubject

    init {
        app.triggerSystemComponent.inject(this)

        subscriptions.add(
                triggerAlarmManager.makeNextAlarmTimeObservable(trigger.objectId!!).subscribe { alarmTime ->
                    println("next alarm time was changed: ${alarmTime}")
                    nextAlarmTimeSubject.onNextIfDifferAndNotNull(alarmTime)
                }
        )
    }

    override fun onSwitchChanged(isOn: Boolean) {
    }

    override fun afterTriggerFired(triggerTime: Long) {
    }

    override fun refreshDaoToFront(snapshot: OTTriggerDAO) {
        onSwitchChanged(snapshot.isOn)
    }

    override fun onDispose() {
        subscriptions.clear()
    }
}