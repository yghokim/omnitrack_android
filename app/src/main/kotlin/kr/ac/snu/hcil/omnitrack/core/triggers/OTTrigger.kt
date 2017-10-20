package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.*
import io.reactivex.subjects.PublishSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTNotificationTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.utils.ListDelta
import kr.ac.snu.hcil.omnitrack.utils.ReadOnlyPair
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.stringKeyEntryParser
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger(objectId: String?, val user: OTUser, name: String, trackerObjectIds: Array<String>?,
                         isOn: Boolean,
                         val action: Int,
                         lastTriggeredTime: Long?, propertyData: Map<String, String>? = null) : NamedObject(objectId, name) {

    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1

        const val TYPE_TIME = 0
        const val TYPE_NEW_ENTRY = 1
        const val TYPE_DATA_THRESHOLD = 2

        const val TRIGGER_TIME_NEVER_TRIGGERED = -1L

        const val PROPERTY_IS_ON = "on"
        const val PROPERTY_LAST_TRIGGERED_TIME = "lastTriggeredTime"
        const val PROPERTY_PROPERTY_DATA = "properties"
        const val PROPERTY_ATTACHED_TRACKERS = "trackers"


        fun makeInstance(objectId: String?, typeId: Int, user: OTUser, name: String, trackerObjectIds: Array<String>?, isOn: Boolean, action: Int, lastTriggeredTime: Long?, propertyData: Map<String, String>?): OTTrigger {
            return when (typeId) {
                TYPE_TIME -> OTTimeTrigger(objectId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, propertyData)
                TYPE_DATA_THRESHOLD -> OTDataTrigger(objectId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, propertyData)
                else -> throw Exception("wrong trigger type : $typeId")
            }
        }

        fun makeInstance(typeId: Int, name: String, action: Int, user: OTUser, vararg trackers: OTTracker): OTTrigger {
            return makeInstance(null, typeId, user, name, trackers.map { it.objectId }.toTypedArray(), false, action, TRIGGER_TIME_NEVER_TRIGGERED, null)
        }

        val localSettingsPreferences: SharedPreferences by lazy {
            OTApp.instance.getSharedPreferences("Trigger_local_settings", Context.MODE_PRIVATE)
        }
    }

    override fun makeNewObjectId(): String {
        return ""
    }

    val triggerAction: OTTriggerAction

    val databasePointRef: DatabaseReference?
        get() = null

    private var currentDbRef: DatabaseReference?
    private val databaseEventListener: ChildEventListener

    abstract val typeId: Int
    abstract val typeNameResourceId: Int
    abstract val descriptionResourceId: Int

    val trackers: List<OTTracker>
        get() = _trackerList

    private val _trackerList = ArrayList<OTTracker>()

    val fired = (PublishSubject.create<ReadOnlyPair<OTTrigger, Long>>())

    val switchTurned = (PublishSubject.create<Boolean>())
    val propertyChanged = (PublishSubject.create<ReadOnlyPair<String, Any?>>())

    val attachedTrackersChanged = (PublishSubject.create<ListDelta<OTTracker>>())

    var isActivatedOnSystem: Boolean = false
        private set

    abstract val configIconId: Int
    abstract val configTitleId: Int

    var lastTriggeredTime: Long? = lastTriggeredTime
        set(value) {

            println("lasttrigger time old: ${field}, new: ${value}")
            if (field != value) {
                field = value
                if (!suspendDatabaseSync) {
                    databasePointRef?.child(PROPERTY_LAST_TRIGGERED_TIME)?.setValue(value)
                }
            }
        }

    var isOn: Boolean by Delegates.observable(isOn) {
        prop, old, new ->

        if (old != new) {
            if (new) {
                this.lastTriggeredTime = null
                handleOn()
            } else {
                handleOff()
            }

            if (!suspendDatabaseSync) {
                databasePointRef?.child(PROPERTY_IS_ON)?.setValue(new)
            }

            switchTurned.onNext(new)
        }
    }

    internal val properties = HashMap<String, Any?>()


    init {
        suspendDatabaseSync = true
        if (trackerObjectIds != null) {
            for (trackerId in trackerObjectIds) {
                val tracker = user[trackerId]
                if (tracker != null) {
                    _trackerList.add(tracker)
                }
            }
        }

        /*
        if(serializedProperties!=null)
        {
            stringKeyEntryParser.fromJson(serializedProperties, Array<SerializedStringKeyEntry>::class.java).forEach {
                properties[it.key] = TypeStringSerializationHelper.deserialize(it.value)
            }
        }*/

        if (propertyData != null) {
            for (child in propertyData) {
                val serializedValue = child.value
                if (serializedValue is String && !serializedValue.isNullOrEmpty()) {
                    properties[child.key] = TypeStringSerializationHelper.deserialize(serializedValue)
                } else properties[child.key] = null
            }
        }

        triggerAction = OTNotificationTriggerAction()

        currentDbRef = databasePointRef
        databaseEventListener = object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {

            }

            private fun handleChildChange(snapshot: DataSnapshot, remove: Boolean) {
                println("trigger child ${snapshot.key} was changed")
                when (snapshot.key) {
                    PROPERTY_IS_ON ->
                        if (remove) {
                            this@OTTrigger.isOn = false
                        } else {
                            this@OTTrigger.isOn = snapshot.value as Boolean
                        }

                /*
                PROPERTY_LAST_TRIGGERED_TIME ->
                    if (remove) {
                        this@OTTrigger.lastTriggeredTime = -1
                    } else {
                        this@OTTrigger.lastTriggeredTime = snapshot.value.toString().toLong()
                    }*/
                    PROPERTY_PROPERTY_DATA ->
                        if (remove) {
                            //TODO set properties to intitial values
                        } else {
                            val propertyDict = snapshot.value as? HashMap<String, String>
                            if (propertyDict != null) {
                                for (child in propertyDict) {
                                    val serializedValue = child.value
                                    if (serializedValue is String && !serializedValue.isNullOrEmpty()) {
                                        properties[child.key] = TypeStringSerializationHelper.deserialize(serializedValue)
                                    } else properties[child.key] = null
                                }
                            }
                        }

                /*
                PROPERTY_ATTACHED_TRACKERS->
                {
                    if(remove){
                        _trackerList.clear()
                        attachedTrackersChanged.onNext(ListDelta())
                    }
                    else{
                        _trackerList.clear()
                        println("tracker list db type:")
                        println(snapshot.value)
                        val list = snapshot.value as? List<HashMap<String, Any>>
                        if(list != null)
                        {
                            for(id in list)
                            {
                                val tracker = user[id["key"].toString()]
                                if(tracker!=null)
                                {
                                    addTracker(tracker)
                                }
                            }
                        }
                    }
                }*/
                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChild: String?) {
                handleChildChange(snapshot, false)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChild: String?) {
                handleChildChange(snapshot, false)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChild: String?) {
                handleChildChange(snapshot, false)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                handleChildChange(snapshot, true)
            }

        }

        currentDbRef?.addChildEventListener(databaseEventListener)

        suspendDatabaseSync = false
    }

    fun notifyPropertyChanged(propertyName: String, value: Any?) {
        propertyChanged.onNext(ReadOnlyPair(propertyName, value))
    }

    internal fun syncPropertyToDatabase(propertyName: String, value: Any?) {
        if (!suspendDatabaseSync)
            databasePointRef?.child(PROPERTY_PROPERTY_DATA)?.child(propertyName)
                    ?.setValue(value?.run { TypeStringSerializationHelper.serialize(value) })
    }

    internal fun getTrackerIdDatabaseChild(dbKey: String?): DatabaseReference? {
        return if (dbKey == null) {
            databasePointRef?.child("trackers")?.push()
        } else {
            databasePointRef?.child("trackers")?.child(dbKey)
        }
    }

    fun getSerializedProperties(): String {
        val list = ArrayList<SerializedStringKeyEntry>()

        for ((key, value) in properties)
            if (value != null) {
                list.add(SerializedStringKeyEntry(key, TypeStringSerializationHelper.serialize(value)))
            }

        return stringKeyEntryParser.toJson(list.toTypedArray())
    }

    fun writePropertiesToDatabase(propertyRef: MutableMap<String, String>) {
        for ((key, value) in properties) {
            if (value != null) {
                propertyRef[key] = TypeStringSerializationHelper.serialize(value)
            }
        }
    }

    fun writePropertiesToDatabase(propertyRef: MutableData) {
        for ((key, value) in properties) {
            if (value != null) {
                propertyRef.child(key).value = TypeStringSerializationHelper.serialize(value)
            }
        }
    }

    fun addTracker(tracker: OTTracker) {
        if (!_trackerList.contains(tracker)) {
            _trackerList.add(tracker)
            if (!suspendDatabaseSync) {
                if (!suspendDatabaseSync) {
                }
            }

            attachedTrackersChanged.onNext(ListDelta())
        }
    }

    fun addTracker(trackerId: String) {
        val tracker = user[trackerId]
        if (tracker != null) {
            addTracker(tracker)
        }
    }

    fun removeTracker(tracker: OTTracker) {
        if (_trackerList.remove(tracker)) {

            if (!suspendDatabaseSync) {
            }

            attachedTrackersChanged.onNext(ListDelta())
        }
    }

    /*
    fun fire(triggerTime: Long, context: Context): Observable<OTTrigger> {
        return Observable.defer<OTTrigger> {
            handleFire(triggerTime)
            triggerAction.performAction(triggerTime, context)
        }.doOnSubscribe {
            fired.onNext(ReadOnlyPair(this, triggerTime))
            this.lastTriggeredTime = triggerTime
        }
    }*/

    fun activateOnSystem(context: Context) {
        if (!isActivatedOnSystem) {
            handleActivationOnSystem(context)
            isActivatedOnSystem = true
        }
    }

    abstract fun handleActivationOnSystem(context: Context)

    open fun handleFire(triggerTime: Long) {
    }


    abstract fun handleOn()
    abstract fun handleOff()

    fun detachFromSystem() {
        currentDbRef?.removeEventListener(databaseEventListener)
        onDetachFromSystem()
    }

    abstract fun onDetachFromSystem()


    override fun save() {

    }
}