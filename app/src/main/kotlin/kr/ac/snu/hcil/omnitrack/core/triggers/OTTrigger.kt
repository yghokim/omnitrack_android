package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger(objectId: String?, dbId: Long?, name: String,
                         trackerObjectIds: Array<String>,
                         isOn: Boolean,
                         action: Int,
                         lastTriggeredTime: Long,
                         serializedProperties: String? = null) : NamedObject(objectId, dbId, name) {


    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1

        const val TYPE_TIME = 0
        const val TYPE_NEW_ENTRY = 1
        const val TYPE_DATA_THRESHOLD = 2

        const val TRIGGER_TIME_NEVER_TRIGGERED = -1L

        fun makeInstance(objectId: String?, dbId: Long?, typeId: Int, name: String, trackerObjectIds: Array<String>, isOn: Boolean, action: Int, lastTriggeredTime: Long, serializedProperties: String?): OTTrigger {
            return when (typeId) {
                TYPE_TIME -> OTTimeTrigger(objectId, dbId, name, trackerObjectIds, isOn, action, lastTriggeredTime, serializedProperties)
                TYPE_DATA_THRESHOLD -> OTDataTrigger(objectId, dbId, name, trackerObjectIds, isOn, action, lastTriggeredTime, serializedProperties)
                else -> throw Exception("wrong trigger type : $typeId")
            }
        }

        fun makeInstance(typeId: Int, name: String, action: Int, vararg trackers: OTTracker): OTTrigger {
            return makeInstance(null, null, typeId, name, trackers.map { it.objectId }.toTypedArray(), false, action, TRIGGER_TIME_NEVER_TRIGGERED, null)
        }
    }

    abstract val typeId: Int
    abstract val typeNameResourceId: Int
    abstract val descriptionResourceId: Int

    val trackers: List<OTTracker>
        get() = _trackerList

    private val _trackerList = ArrayList<OTTracker>()

    val fired = Event<Long>()

    val switchTurned = Event<Boolean>()

    var isActivatedOnSystem: Boolean = false
        private set

    abstract val configIconId: Int
    abstract val configTitleId: Int


    val action: Int = action

    var lastTriggeredTime: Long by Delegates.observable(lastTriggeredTime) {
        prop, old, new ->
        if (old != new) {
            isDirtySinceLastSync = true
        }
    }

    var isOn: Boolean by Delegates.observable(isOn) {
        prop, old, new ->
        if (new) {
            this.lastTriggeredTime = -1L
            handleOn()
        } else {
            handleOff()
        }

        if (old != new) {

            isDirtySinceLastSync = true

            switchTurned.invoke(this, new)
        }
    }

    protected val properties = HashMap<String, Any?>()


    init {
        for (trackerId in trackerObjectIds) {
            val tracker = OTApplication.app.currentUser[trackerId]
            if (tracker != null) {
                _trackerList.add(tracker)
            }
        }

        if(serializedProperties!=null)
        {
            val gson = Gson()
            val parcel = gson.fromJson(serializedProperties, Array<SerializedStringKeyEntry>::class.java)
            for(entry in parcel)
            {
                properties[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
            }
        }

        isDirtySinceLastSync = true
    }

    fun getSerializedProperties(): String{
        val list = ArrayList<SerializedStringKeyEntry>()
        for(entry in properties)
        {
            if(entry.value!=null)
            {
                list.add(SerializedStringKeyEntry(entry.key, TypeStringSerializationHelper.serialize(entry.value!!)))
            }
        }

        return Gson().toJson(list.toTypedArray())
    }

    fun addTracker(tracker: OTTracker) {
        if (!_trackerList.contains(tracker)) {
            _trackerList.add(tracker)
            isDirtySinceLastSync = true
        }
    }

    fun addTracker(trackerId: String) {
        val tracker = OTApplication.app.currentUser[trackerId]
        if (tracker != null) {
            addTracker(tracker)
        }
    }

    fun removeTracker(tracker: OTTracker) {
        if (_trackerList.remove(tracker)) {
            isDirtySinceLastSync = true
        }
    }

    fun fire(triggerTime: Long, finished: ((success: Boolean) -> Unit)? = null) {
        handleFire(triggerTime)

        when (action) {
            OTTrigger.ACTION_BACKGROUND_LOGGING -> {
                println("trigger fired - loggin in background")

                //Toast.makeText(OTApplication.app, "Logged!", Toast.LENGTH_SHORT).show()

                var left = trackers.size
                for (tracker in trackers)
                    OTBackgroundLoggingService.startLoggingAsync(OTApplication.app, tracker, OTBackgroundLoggingService.LoggingSource.Trigger) {
                        left--
                        if (left == 0) {
                            finished?.invoke(true)
                        }
                    }
            }
            OTTrigger.ACTION_NOTIFICATION -> {
                println("trigger fired - send notification")
                for (tracker in trackers)
                    OTNotificationManager.pushReminderNotification(OTApplication.app, tracker, triggerTime)
                finished?.invoke(true)
            }
        }

        fired.invoke(this, triggerTime)
        this.lastTriggeredTime = triggerTime
    }

    fun activateOnSystem(context: Context) {
        if (!isActivatedOnSystem) {
            handleActivationOnSystem(context)
            isActivatedOnSystem = true
        }
    }

    abstract fun handleActivationOnSystem(context: Context)

    open fun handleFire(triggerTime: Long) {
    }


    abstract fun handleOn();
    abstract fun handleOff();

}