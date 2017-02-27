package kr.ac.snu.hcil.omnitrack.core.triggers

import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseHelper
import rx.internal.util.SubscriptionList
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTTriggerManager(val user: OTUser) {

    private val subscriptions = SubscriptionList()

    init {
        subscriptions.add(
                user.trackerAdded.subscribe {
                    trackerPair ->

                })

        subscriptions.add(
                user.trackerRemoved.subscribe {
                    trackerPair ->
                    for (trigger in getAttachedTriggers(trackerPair.first).clone()) {
                        //removeTrigger(trigger)
                        trigger.removeTracker(trackerPair.first)
            }
                })
    }

    constructor(user: OTUser, loadedTriggers: List<OTTrigger>?) : this(user) {

        if (loadedTriggers != null) {

            println("${loadedTriggers.size} triggers are loaded.")

            triggers += loadedTriggers

            triggers.forEach {
                it.fired += triggerFiredHandler
                it.activateOnSystem(OTApplication.app.applicationContext)
            }
        }
    }

    private val triggers = ArrayList<OTTrigger>()

    private val trackerPivotedTriggerListCache = Hashtable<String, Array<OTTrigger>>()

    val triggerAdded = SerializedSubject(PublishSubject.create<OTTrigger>())
    val triggerRemoved = SerializedSubject(PublishSubject.create<OTTrigger>())

    private val triggerFiredHandler = {
        sender: Any, triggerTime: Long ->
        onTriggerFired(sender as OTTrigger, triggerTime)
    }

    fun getTriggerWithId(objId: String): OTTrigger? {
        return triggers.find { it.objectId == objId }
    }

    fun getFilteredTriggers(filter: (OTTrigger)->Boolean): List<OTTrigger>{
        return triggers.filter{
            filter(it)==true
        }
    }

    fun getAttachedTriggers(tracker: OTTracker): Array<OTTrigger> {
        if (trackerPivotedTriggerListCache.containsKey(tracker.objectId)) {
            //println("triggers cache hit")
        } else {
            //println("triggers cache miss")
            trackerPivotedTriggerListCache.set(tracker.objectId,
                    triggers.filter { it.trackers.contains(tracker) }.toTypedArray())
        }

        return trackerPivotedTriggerListCache[tracker.objectId]!!
    }

    fun getAttachedTriggers(tracker: OTTracker, action: Int): Array<OTTrigger> {
        return getAttachedTriggers(tracker).filter { it.action == action }.toTypedArray()
    }

    fun getTriggersOfAction(action: Int): Array<OTTrigger> {
        return triggers.filter { it.action == action }.toTypedArray()
    }

    fun detachFromSystem() {
        for (trigger in triggers) {
            trigger.detachFromSystem()
        }
        subscriptions.clear()
    }

    fun putNewTrigger(trigger: OTTrigger) {
        if (triggers.find { it.objectId == trigger.objectId } == null) {
            triggers.add(trigger)
            trigger.fired += triggerFiredHandler

            if (trigger.isOn) {
                trigger.activateOnSystem(OTApplication.app.applicationContext)
            }

            for (tracker in trigger.trackers) {
                if (trackerPivotedTriggerListCache.containsKey(tracker.objectId)) {
                    trackerPivotedTriggerListCache[tracker.objectId] =
                            trackerPivotedTriggerListCache[tracker.objectId]!! + trigger
                }
            }

            triggerAdded.onNext(trigger)
        }
    }

    fun removeTrigger(trigger: OTTrigger) {
        triggers.remove(trigger)
        trigger.fired -= triggerFiredHandler

        trigger.suspendDatabaseSync = true
        trigger.isOn = false

        //TODO handler dependencies associated with the trigger
        if (trigger is OTTimeTrigger) {
            OTApplication.app.timeTriggerAlarmManager.cancelTrigger(trigger)
        }

        for (tracker in trigger.trackers) {
            if (trackerPivotedTriggerListCache.containsKey(tracker.objectId)) {
                trackerPivotedTriggerListCache[tracker.objectId] = trackerPivotedTriggerListCache[tracker.objectId]!!.filter { it != trigger }.toTypedArray()
            }
        }

        //TODO remove trigger from DB
        FirebaseHelper.removeTrigger(trigger)

        triggerRemoved.onNext(trigger)
    }

    private fun onTriggerFired(trigger: OTTrigger, triggerTime: Long) {

    }

    operator fun iterator(): MutableIterator<OTTrigger> {
        return triggers.iterator()
    }

    fun withIndex(): Iterable<IndexedValue<OTTrigger>> {
        return triggers.withIndex()
    }



}