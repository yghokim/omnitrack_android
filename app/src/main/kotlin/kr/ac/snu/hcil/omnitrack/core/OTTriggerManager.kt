package kr.ac.snu.hcil.omnitrack.core

import android.widget.Toast
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import java.util.*

/**
 * Created by younghokim on 16. 7. 28..
 */
class OTTriggerManager(val user: OTUser) {

    constructor(user: OTUser, loadedTriggers: List<OTTrigger>?) : this(user) {
        if (loadedTriggers != null) {
            triggers += loadedTriggers

            triggers.forEach {
                it.fired += triggerFiredHandler
                it.activateOnSystem(OmniTrackApplication.app.applicationContext)
            }
        }
    }


    private val _removedTriggerIds = ArrayList<Long>()
    fun fetchRemovedTriggerIds(): LongArray {
        val result = _removedTriggerIds.toLongArray()
        _removedTriggerIds.clear()
        return result;
    }

    private val triggers = ArrayList<OTTrigger>()

    private val trackerPivotedTriggerListCache = Hashtable<String, Array<OTTrigger>>()

    private val triggerFiredHandler = {
        sender: Any, action: Int ->
        onTriggerFired(sender as OTTrigger, action)
    }

    fun getTriggerWithId(objId: String): OTTrigger? {
        return triggers.find { it.objectId == objId }
    }

    fun getAttachedTriggers(tracker: OTTracker): Array<OTTrigger> {
        if (trackerPivotedTriggerListCache.containsKey(tracker.objectId)) {
            //println("triggers cache hit")
        } else {
            //println("triggers cache miss")
            trackerPivotedTriggerListCache.set(tracker.objectId,
                    triggers.filter { it.trackerObjectId == tracker.objectId }.toTypedArray())
        }

        return trackerPivotedTriggerListCache[tracker.objectId]!!
    }

    fun putNewTrigger(trigger: OTTrigger) {
        if (triggers.find { it.objectId == trigger.objectId } == null) {
            triggers.add(trigger)
            trigger.fired += triggerFiredHandler

            if (trigger.isOn) {
                trigger.activateOnSystem(OmniTrackApplication.app.applicationContext)
            }

            if (_removedTriggerIds.contains(trigger.dbId)) {
                _removedTriggerIds.remove(trigger.dbId)
            }

            if (trackerPivotedTriggerListCache.containsKey(trigger.trackerObjectId)) {
                trackerPivotedTriggerListCache[trigger.trackerObjectId] =
                        trackerPivotedTriggerListCache[trigger.trackerObjectId]!! + trigger
            }
        }
    }

    fun removeTrigger(trigger: OTTrigger) {
        triggers.remove(trigger)
        trigger.fired -= triggerFiredHandler

        trigger.isOn = false

        if (trigger.dbId != null)
            _removedTriggerIds.add(trigger.dbId!!)


        //TODO handler dependencies associated with the trigger


        if (trackerPivotedTriggerListCache.containsKey(trigger.trackerObjectId)) {
            trackerPivotedTriggerListCache[trigger.trackerObjectId] = trackerPivotedTriggerListCache[trigger.trackerObjectId]!!.filter { it != trigger }.toTypedArray()
        }
    }

    private fun onTriggerFired(trigger: OTTrigger, action: Int) {
        when (action) {
            OTTrigger.ACTION_BACKGROUND_LOGGING -> {
                println("trigger fired - loggin in background")

                Toast.makeText(OmniTrackApplication.app, "Logged!", Toast.LENGTH_SHORT).show()
                OTBackgroundLoggingService.startLogging(OmniTrackApplication.app, trigger.tracker)
            }
            OTTrigger.ACTION_NOTIFICATION -> {
                println("trigger fired - send notification")
            }
        }


    }

    operator fun iterator(): MutableIterator<OTTrigger> {
        return triggers.iterator()
    }

    fun withIndex(): Iterable<IndexedValue<OTTrigger>> {
        return triggers.withIndex()
    }



}