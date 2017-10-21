package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.legacy

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger

/**
 * Created by Young-Ho on 6/4/2017.
 */
open class TriggerViewModel<out T>(internal val trigger: T) where T : OTTrigger {

    protected val subscriptions = CompositeDisposable()

    val triggerAction: BehaviorSubject<Int> = BehaviorSubject.create()
    val triggerType: BehaviorSubject<Int> = BehaviorSubject.create()
    val triggerId: BehaviorSubject<String> = BehaviorSubject.create()

    val triggerConfigIconResId: BehaviorSubject<Int> = BehaviorSubject.create()
    val triggerDescResId: BehaviorSubject<Int> = BehaviorSubject.create()


    val triggerSwitch: BehaviorSubject<Boolean> = BehaviorSubject.create()

    val triggerConfigSummary: BehaviorSubject<CharSequence> = BehaviorSubject.create()

    val attachedTriggers: BehaviorSubject<List<OTTracker>> = BehaviorSubject.create()
    private val _currentAttachedTriggers = ArrayList<OTTracker>()
    val currentAttachedTrackers: List<OTTracker> get() = _currentAttachedTriggers

    open fun register() {
        triggerAction.onNext(trigger.action)
        triggerType.onNext(trigger.typeId)
        triggerConfigIconResId.onNext(trigger.configIconId)
        triggerDescResId.onNext(trigger.descriptionResourceId)
        triggerId.onNext(trigger.objectId)

        triggerSwitch.onNext(trigger.isOn)
        subscriptions.add(
                trigger.switchTurned.subscribe {
                    isOn ->
                    println("trigger switch changed: ${isOn}")
                    triggerSwitch.onNext(isOn)
                }
        )


        syncTrackerListFromTrigger()

        subscriptions.add(
                trigger.attachedTrackersChanged.subscribe {
                    delta ->
                    syncTrackerListFromTrigger()
                }
        )

    }

    open fun unregister() {
        subscriptions.clear()
        _currentAttachedTriggers.clear()
        attachedTriggers.onNext(emptyList())
    }

    private fun syncTrackerListFromTrigger() {

        _currentAttachedTriggers.clear()
        val list = trigger.trackers
        _currentAttachedTriggers.addAll(list)
        attachedTriggers.onNext(list)
    }

    fun setTriggerSwitch(switchOn: Boolean) {
        trigger.isOn = switchOn
    }

    fun attachTracker(tracker: OTTracker) {
        trigger.addTracker(tracker)
    }

    fun detachTracker(tracker: OTTracker) {
        trigger.removeTracker(tracker)
    }
}