package kr.ac.snu.hcil.omnitrack.core

import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTTriggerManager(val user: OTUser) {

    private val triggers = ArrayList<OTTrigger>()

    private val trackerPivotedTriggerListCache = Hashtable<String, Array<OTTrigger>>()

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
            if (trackerPivotedTriggerListCache.containsKey(trigger.trackerObjectId)) {
                trackerPivotedTriggerListCache[trigger.trackerObjectId] =
                        trackerPivotedTriggerListCache[trigger.trackerObjectId]!! + trigger
            } else {
                trackerPivotedTriggerListCache[trigger.trackerObjectId] = arrayOf(trigger)
            }
        }
    }

}