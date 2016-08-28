package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger(objectId: String?, dbId: Long?, name: String,
                         trackerObjectId: String,
                         isOn: Boolean,
                         lastTriggeredTime: Long,
                         serializedProperties: String? = null) : NamedObject(objectId, dbId, name) {


    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1

        const val TYPE_TIME = 0
        const val TYPE_NEW_ENTRY = 1
        const val TYPE_SERVICE_EVENT = 2

        const val TRIGGER_TIME_NEVER_TRIGGERED = -1L

        fun makeInstance(objectId: String?, dbId: Long?, typeId: Int, name: String, trackerObjectId: String, isOn: Boolean, lastTriggeredTime: Long, serializedProperties: String?): OTTrigger {
            return when (typeId) {
                TYPE_TIME -> OTTimeTrigger(objectId, dbId, name, trackerObjectId, isOn, lastTriggeredTime, serializedProperties)
                else -> throw Exception("wrong trigger type : $typeId")
            }
        }

        fun makeInstance(typeId: Int, name: String, tracker: OTTracker): OTTrigger {
            return makeInstance(null, null, typeId, name, tracker.objectId, false, TRIGGER_TIME_NEVER_TRIGGERED, null)
        }
    }

    abstract val typeId: Int
    abstract val typeNameResourceId: Int
    abstract val descriptionResourceId: Int

    val trackerObjectId: String

    val tracker: OTTracker

    val fired = Event<Int>()

    val switchTurned = Event<Boolean>()

    var isActivatedOnSystem: Boolean = false
        private set

    abstract val configIconId: Int
    abstract val configTitleId: Int


    var action: Int by Delegates.observable(ACTION_BACKGROUND_LOGGING)
    {
        prop, old, new ->
        if (new < ACTION_NOTIFICATION && new > ACTION_BACKGROUND_LOGGING) {
            throw Exception("Wrong trigger action code.")
        }
    }

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
        this.trackerObjectId = trackerObjectId
        this.tracker = OTApplication.app.currentUser[trackerObjectId]!!

        if(serializedProperties!=null)
        {
            val gson = Gson()
            val parcel = gson.fromJson(serializedProperties, Array<SerializedStringKeyEntry>::class.java)
            for(entry in parcel)
            {
                properties[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
            }
        }

        isDirtySinceLastSync = false
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


    fun fire(triggerTime: Long) {
        handleFire(triggerTime)

        fired.invoke(this, action)
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