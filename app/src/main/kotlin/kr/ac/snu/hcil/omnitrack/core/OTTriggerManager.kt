package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTTriggerManager(val user: OTUser) {

    constructor(user: OTUser, loadedTriggers: List<OTTrigger>?) : this(user) {
        if (loadedTriggers != null) {
            triggers += loadedTriggers

            triggers.forEach { it.fired += triggerFiredHandler }
        }
    }

    private val triggers = ArrayList<OTTrigger>()

    private val trackerPivotedTriggerListCache = Hashtable<String, Array<OTTrigger>>()

    private val triggerFiredHandler = {
        sender: Any, args: OTTracker ->
        onTriggerFired(sender as OTTrigger)
    }

    fun getAttachedTriggers(tracker: OTTracker): Array<OTTrigger> {
        if (trackerPivotedTriggerListCache.containsKey(tracker.objectId)) {
            println("triggers cache hit")
        } else {
            println("triggers cache miss")
            trackerPivotedTriggerListCache.set(tracker.objectId,
                    triggers.filter { it.trackerObjectId == tracker.objectId }.toTypedArray())
        }

        return trackerPivotedTriggerListCache[tracker.objectId]!!
    }

    fun putNewTrigger(trigger: OTTrigger) {
        if (triggers.find { it.objectId == trigger.objectId } == null) {
            triggers.add(trigger)
            trigger.fired += triggerFiredHandler

            if (trackerPivotedTriggerListCache.containsKey(trigger.trackerObjectId)) {
                trackerPivotedTriggerListCache[trigger.trackerObjectId] =
                        trackerPivotedTriggerListCache[trigger.trackerObjectId]!! + trigger
            }
        }
    }

    fun removeTrigger(trigger: OTTrigger) {
        triggers.remove(trigger)
        trigger.fired -= triggerFiredHandler

        if (trackerPivotedTriggerListCache.containsKey(trigger.trackerObjectId)) {
            trackerPivotedTriggerListCache[trigger.trackerObjectId] = trackerPivotedTriggerListCache[trigger.trackerObjectId]!!.filter { it != trigger }.toTypedArray()
        }
    }

    private fun onTriggerFired(trigger: OTTrigger) {
        println("trigger fired!")
    }

    operator fun iterator(): MutableIterator<OTTrigger> {
        return triggers.iterator()
    }

    fun withIndex(): Iterable<IndexedValue<OTTrigger>> {
        return triggers.withIndex()
    }



}