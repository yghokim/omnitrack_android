package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.IDatabaseStorable
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger : NamedObject {


    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1

        const val TYPE_PERIODIC = 0
        const val TYPE_NEW_ENTRY = 1
        const val TYPE_SERVICE_EVENT = 2

        fun makeInstance(objectId: String?, dbId: Long?, typeId: Int, name: String, trackerObjectId: String, serializedProperties: String?): OTTrigger {
            return when (typeId) {
                TYPE_PERIODIC -> OTPeriodicTrigger(objectId, dbId, name, trackerObjectId, serializedProperties)
                else -> throw Exception("wrong trigger type : $typeId")
            }
        }

        fun makeInstance(typeId: Int, name: String, tracker: OTTracker): OTTrigger {
            return makeInstance(null, null, typeId, name, tracker.objectId, null)
        }
    }

    abstract val typeId: Int
    abstract val typeNameResourceId: Int
    abstract val descriptionResourceId: Int

    val trackerObjectId: String

    val tracker: OTTracker

    val fired = Event<Int>()

    var isActivatedOnSystem: Boolean = false
        private set

    var action: Int by Delegates.observable(ACTION_BACKGROUND_LOGGING)
    {
        prop, old, new ->
        if (new < ACTION_NOTIFICATION && new > ACTION_BACKGROUND_LOGGING) {
            throw Exception("Wrong trigger action code.")
        }
    }
    var isOn: Boolean by Delegates.observable(true) {
        prop, old, new ->
        if (new) {
            handleOn()
        } else {
            handleOff()
        }
    }

    protected val properties = HashMap<String, Any?>()


    constructor(name: String, tracker: OTTracker) : super(null, null, name) {
        trackerObjectId = tracker.objectId
        this.tracker = tracker
    }

    constructor(objectId: String?, dbId: Long?, name: String, trackerObjectId: String, serializedProperties: String?=null) : super(objectId, dbId, name) {
        this.trackerObjectId = trackerObjectId
        this.tracker = OmniTrackApplication.app.currentUser[trackerObjectId]!!

        if(serializedProperties!=null)
        {
            val gson = Gson()
            val parcel = gson.fromJson(serializedProperties, Array<SerializedStringKeyEntry>::class.java)
            for(entry in parcel)
            {
                properties[entry.key] = TypeStringSerializationHelper.deserialize(entry.value)
            }
        }
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


    fun fire() {
        handleFire()

        fired.invoke(this, action)
    }

    fun activateOnSystem(context: Context) {
        if (!isActivatedOnSystem) {
            handleActivationOnSystem(context)
            isActivatedOnSystem = true
        }
    }

    abstract fun handleActivationOnSystem(context: Context);

    open fun handleFire() {
    }


    abstract fun handleOn();
    abstract fun handleOff();



}