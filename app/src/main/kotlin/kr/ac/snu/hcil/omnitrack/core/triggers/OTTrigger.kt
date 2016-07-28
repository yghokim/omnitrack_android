package kr.ac.snu.hcil.omnitrack.core.triggers

import com.google.gson.Gson
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.NamedObject
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.IDatabaseStorable
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
    }

    abstract val typeId: Int
    abstract val nameResourceId: Int
    abstract val descriptionResourceId: Int

    val trackerObjectId: String

    val tracker: OTTracker

    var action: Int by Delegates.observable(ACTION_NOTIFICATION)
    {
        prop, old, new ->

    }
    var isActive: Boolean by Delegates.observable(true) {
        prop, old, new ->

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

}