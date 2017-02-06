package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.NamedObject
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.services.OTBackgroundLoggingService
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializedStringKeyEntry
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.stringKeyEntryParser
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*
import kotlin.properties.Delegates

/**
 * Created by younghokim on 16. 7. 27..
 */
abstract class OTTrigger(objectId: String?, dbId: Long?, val user: OTUser, name: String,
                         trackerObjectIds: Array<String>,
                         isOn: Boolean,
                         val action: Int,
                         lastTriggeredTime: Long,
                         serializedProperties: String? = null) : NamedObject(objectId, dbId, name) {


    companion object {
        const val ACTION_NOTIFICATION = 0
        const val ACTION_BACKGROUND_LOGGING = 1

        const val TYPE_TIME = 0
        const val TYPE_NEW_ENTRY = 1
        const val TYPE_DATA_THRESHOLD = 2

        const val TRIGGER_TIME_NEVER_TRIGGERED = -1L

        fun makeInstance(objectId: String?, dbId: Long?, typeId: Int, user: OTUser, name: String, trackerObjectIds: Array<String>, isOn: Boolean, action: Int, lastTriggeredTime: Long, serializedProperties: String?): OTTrigger {
            return when (typeId) {
                TYPE_TIME -> OTTimeTrigger(objectId, dbId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, serializedProperties)
                TYPE_DATA_THRESHOLD -> OTDataTrigger(objectId, dbId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, serializedProperties)
                else -> throw Exception("wrong trigger type : $typeId")
            }
        }

        fun makeInstance(typeId: Int, name: String, action: Int, user: OTUser, vararg trackers: OTTracker): OTTrigger {
            return makeInstance(null, null, typeId, user, name, trackers.map { it.objectId }.toTypedArray(), false, action, TRIGGER_TIME_NEVER_TRIGGERED, null)
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
            val tracker = user[trackerId]
            if (tracker != null) {
                _trackerList.add(tracker)
            }
        }

        if(serializedProperties!=null)
        {
            stringKeyEntryParser.fromJson(serializedProperties, Array<SerializedStringKeyEntry>::class.java).forEach {
                properties[it.key] = TypeStringSerializationHelper.deserialize(it.value)
            }
        }

        isDirtySinceLastSync = true
    }

    fun getSerializedProperties(): String{
        val list = ArrayList<SerializedStringKeyEntry>()

        for ((key, value) in properties)
            if (value != null)
            {
                list.add(SerializedStringKeyEntry(key, TypeStringSerializationHelper.serialize(value)))
            }

        return stringKeyEntryParser.toJson(list.toTypedArray())
    }

    fun addTracker(tracker: OTTracker) {
        if (!_trackerList.contains(tracker)) {
            _trackerList.add(tracker)
            isDirtySinceLastSync = true
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
            isDirtySinceLastSync = true
        }
    }

    fun fire(triggerTime: Long): Observable<OTTrigger> {
        return Observable.defer<OTTrigger> {
            handleFire(triggerTime)

            when (action) {
                OTTrigger.ACTION_BACKGROUND_LOGGING -> {
                    println("trigger fired - logging in background")

                    //Toast.makeText(OTApplication.app, "Logged!", Toast.LENGTH_SHORT).show()
                    Observable.create {
                        subscriber ->
                        Observable.merge(trackers.filter { it.isValid(null) }.map { OTBackgroundLoggingService.log(OTApplication.app, it, OTItem.LoggingSource.Trigger).subscribeOn(Schedulers.newThread()) })
                                .subscribe({}, {}, {
                                    if (!subscriber.isUnsubscribed) {
                                        subscriber.onNext(this)
                                        subscriber.onCompleted()
                                    }
                                })
                    }

                }
                OTTrigger.ACTION_NOTIFICATION -> {
                    println("trigger fired - send notification")
                    for (tracker in trackers)
                        OTNotificationManager.pushReminderNotification(OTApplication.app, tracker, triggerTime)
                    Observable.just(this)
                }
                else -> throw Exception("Not supported Trigger type")
            }

        }.doOnSubscribe {
            fired.invoke(this, triggerTime)
            this.lastTriggeredTime = triggerTime
        }
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


    abstract fun handleOn()
    abstract fun handleOff()

    abstract fun detachFromSystem()

}