package kr.ac.snu.hcil.omnitrack.core.database

import android.support.annotation.Keep
import com.google.firebase.database.*
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 2017. 2. 9..
 */
object FirebaseHelper {
    const val CHILD_NAME_USERS = "users"
    const val CHILD_NAME_TRACKERS = "trackers"
    const val CHILD_NAME_ATTRIBUTES = "attributes"
    const val CHILD_NAME_TRIGGERS = "triggers"

    const val CHILD_NAME_EXPERIMENT_PROFILE = "experiment_profile"

    private val fbInstance: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    init {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }

    val dbRef: DatabaseReference? get() = fbInstance.reference

    val currentUserRef: DatabaseReference? get() = dbRef?.child(CHILD_NAME_USERS)?.child(OTAuthManager.userId)

    val experimentProfileRef: DatabaseReference? get() = currentUserRef?.child(CHILD_NAME_EXPERIMENT_PROFILE)

    private fun trackerRef(trackerId: String): DatabaseReference? {
        return dbRef?.child(CHILD_NAME_TRACKERS)?.child(trackerId)
    }

    private fun triggerRef(triggerId: String): DatabaseReference? {
        return dbRef?.child(CHILD_NAME_TRIGGERS)?.child(triggerId)
    }

    open class NamedPOJO {
        var name: String? = null
    }

    @Keep
    class TrackerPOJO : NamedPOJO() {
        var user: String? = null
        var position: Int = 0
        var color: Int = 0
        var attributeLocalKeySeed: Int = 0
        var onShortcut: Boolean = false
    }

    @Keep
    class AttributePOJO : NamedPOJO() {
        var localKey: Int = -1
        var position: Int = 0
        var propertySerialized: String? = null
        var connectionSerialized: String? = null
        var type: Int = 0
        var required: Boolean = false
    }

    @Keep
    class TriggerPOJO : NamedPOJO() {
        var user: String? = null
        var position: Int = 0
        var action: Int = 0
        var type: Int = 0
        var on: Boolean = false
        var lastTriggeredTime: Long = 0
    }

    @Keep
    class IndexedKey(
            var position: Int = 0,
            var key: String? = null) {
    }

    fun generateNewKey(childName: String): String {
        val newKey = dbRef!!.child(childName).push().key
        println("New Firebase Key: ${newKey}")
        return newKey
    }

    fun generateAttributeKey(trackerId: String): String {
        return trackerRef(trackerId)!!.child(CHILD_NAME_ATTRIBUTES)!!.push().key
    }

    fun saveUser(user: OTUser) {
        for (child in user.trackers.iterator().withIndex()) {
            saveTracker(child.value, child.index)
        }

        for (triggerEntry in user.triggerManager.withIndex()) {
            saveTrigger(triggerEntry.value, user.objectId, triggerEntry.index)
        }
    }

    fun saveTrigger(trigger: OTTrigger, userId: String, position: Int) {
        val pojo = TriggerPOJO()
        pojo.action = trigger.action
        pojo.name = trigger.name
        pojo.on = trigger.isOn
        pojo.lastTriggeredTime = trigger.lastTriggeredTime
        pojo.position = position
        pojo.type = trigger.typeId
        pojo.user = userId

        val ref = triggerRef(triggerId = trigger.objectId)
        if (ref != null) {
            ref.setValue(pojo)
            ref.child("trackers").setValue(trigger.trackers.mapIndexed { i, tracker -> IndexedKey(i, tracker.objectId) })
            trigger.writePropertiesToDatabase(ref.child("properties"))
        }
    }

    fun findTriggersOfUser(user: OTUser): Observable<List<OTTrigger>> {
        println("userId: ${user.objectId}")
        val query = //makeQueryOfUser(user.objectId, CHILD_NAME_TRIGGERS)
                dbRef?.child("triggers")?.orderByChild("user")?.equalTo(user.objectId)
        return if (query != null) {
            Observable.create {
                subscriber ->
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        error.toException().printStackTrace()
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(error.toException())
                        }
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        println("Trigger snapshot exists ; ${snapshot.exists()}")
                        println("Trigger snapthot ${snapshot.value}")
                        val triggers = ArrayList<Pair<Int, OTTrigger>>(snapshot.childrenCount.toInt())

                        println("Trigger count: ${snapshot.childrenCount}")

                        for (child in snapshot.children) {
                            val pojo = child.getValue(TriggerPOJO::class.java)
                            if (pojo != null) {

                                val trackerIds = ArrayList<Pair<String, IndexedKey>>()
                                for (trackerIdRef in child.child("trackers").children) {
                                    val trackerIdWithPosition = trackerIdRef.getValue(IndexedKey::class.java)
                                    if (trackerIdWithPosition != null) {
                                        trackerIds.add(Pair(trackerIdRef.key, trackerIdWithPosition))
                                    }
                                }
                                trackerIds.sortBy { it -> it.second.position }


                                val trigger = OTTrigger.makeInstance(
                                        child.key,
                                        pojo.type,
                                        user,
                                        pojo.name ?: "",
                                        trackerIds.map { Pair<String?, String>(it.first, it.second.key!!) }.toTypedArray(),
                                        pojo.on, pojo.action, pojo.lastTriggeredTime, child.child("properties")) //TODO properties

                                triggers.add(
                                        Pair(pojo.position, trigger)
                                )
                            }
                        }

                        triggers.sortBy(Pair<Int, OTTrigger>::first)

                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(triggers.map { it -> it.second })
                            subscriber.onCompleted()
                        }
                    }

                })

            }
        } else {
            Observable.error<List<OTTrigger>>(Exception("Firebase db error retrieving triggers."))
        }

    }

    fun removeTracker(tracker: OTTracker, formerOwner: OTUser) {
        println("Firebase remove tracker: ${tracker.name}, ${tracker.objectId}")
        deBelongReference(trackerRef(tracker.objectId), CHILD_NAME_TRACKERS, formerOwner.objectId)
    }

    fun removeTrigger(trigger: OTTrigger) {

        println("Firebase remove tracker: ${trigger.name}, ${trigger.objectId}")
        deBelongReference(triggerRef(trigger.objectId), CHILD_NAME_TRIGGERS, trigger.user.objectId)
    }

    private fun deBelongReference(ref: DatabaseReference?, childName: String, userId: String) {

        ref?.child("removed_at")?.setValue(ServerValue.TIMESTAMP)
        ref?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val trashRef = dbRef?.child("removed")?.child(childName)?.child(snapshot.key)
                trashRef?.setValue(snapshot.value)
                snapshot.ref.removeValue()
            }

        })
    }

    fun removeAttribute(trackerId: String, objectId: String) {
        trackerRef(trackerId)?.child(CHILD_NAME_ATTRIBUTES)?.child(objectId)?.removeValue()
    }

    fun makeQueryOfUser(userId: String, childName: String): Query? {
        return dbRef?.child(childName)?.orderByChild("user")?.equalTo(userId)
    }

    fun extractTrackerWithPosition(snapshot: DataSnapshot): Pair<Int, OTTracker> {
        val pojo = snapshot.getValue(TrackerPOJO::class.java)
        val attributesRef = snapshot.child(CHILD_NAME_ATTRIBUTES)

        val attributeList = ArrayList<OTAttribute<out Any>>()
        val attrPojos = ArrayList<Pair<String, AttributePOJO>>()
        for (attributeRef in attributesRef.children) {

            val attrPojo = attributeRef.getValue(AttributePOJO::class.java)
            attrPojos.add(Pair(attributeRef.key, attrPojo))
        }

        attrPojos.sortBy { it -> it.second.position }

        for (attrPojo in attrPojos) {
            attributeList.add(
                    OTAttribute.createAttribute(
                            attrPojo.first,
                            attrPojo.second.localKey,
                            null,
                            attrPojo.second.name ?: "noname",
                            attrPojo.second.required,
                            attrPojo.second.type,
                            attrPojo.second.propertySerialized,
                            attrPojo.second.connectionSerialized)
            )
        }

        return Pair(pojo.position, OTTracker(
                snapshot.key,
                pojo.name ?: "Noname",
                pojo.color,
                pojo.onShortcut,
                pojo.attributeLocalKeySeed,
                attributeList
        ))
    }

    fun findTrackersOfUser(userId: String): Observable<List<OTTracker>> {

        println("userId: ${userId}")
        val query = makeQueryOfUser(userId, CHILD_NAME_TRACKERS)
        return if (query != null) {
            Observable.create {
                subscriber ->
                query.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        error.toException().printStackTrace()
                        if (!subscriber.isUnsubscribed) {
                            subscriber.onError(error.toException())
                        }
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {

                        println("Tracker snapshot exists ; ${snapshot.exists()}")
                        println("Tracker snapthot ${snapshot.value}")

                        val trackers = ArrayList<Pair<Int, OTTracker>>(snapshot.childrenCount.toInt())

                        println("Tracker count: ${snapshot.childrenCount}")

                        for (child in snapshot.children) {
                            trackers.add(extractTrackerWithPosition(child))
                        }

                        trackers.sortBy(Pair<Int, OTTracker>::first)

                        if (!subscriber.isUnsubscribed) {
                            subscriber.onNext(trackers.map { it -> it.second })
                            subscriber.onCompleted()
                        }
                    }

                })

            }
        } else {
            Observable.error<List<OTTracker>>(Exception("Firebase db error retrieving trackers."))
        }

    }

    fun saveAttribute(trackerId: String, attribute: OTAttribute<out Any>, position: Int) {
        val attributeRef = trackerRef(trackerId)?.child("attributes")?.child(attribute.objectId)
        val pojo = AttributePOJO()
        pojo.localKey = attribute.localKey
        pojo.position = position
        pojo.required = attribute.isRequired
        pojo.connectionSerialized = attribute.valueConnection?.getSerializedString()
        pojo.propertySerialized = attribute.getSerializedProperties()
        pojo.type = attribute.typeId
        pojo.name = attribute.name

        attributeRef?.setValue(pojo)

    }

    fun saveTracker(tracker: OTTracker, position: Int) {
        println("save tracker: ${tracker.name}, ${tracker.objectId}")
        val values = TrackerPOJO()
        values.name = tracker.name
        values.position = position
        values.color = tracker.color
        values.user = tracker.owner?.objectId
        values.onShortcut = tracker.isOnShortcut
        values.attributeLocalKeySeed = tracker.attributeLocalKeySeed

        val trackerRef = trackerRef(tracker.objectId)

        trackerRef?.setValue(values, DatabaseReference.CompletionListener { p0, p1 ->
            if (p0 != null) {
                p0.toException().printStackTrace()
                println("Firebase error.")
            } else {
                println("No firebase error. completed.")
            }
        })

        for (attribute in tracker.attributes.unObservedList.withIndex()) {
            saveAttribute(tracker.objectId, attribute.value, attribute.index)
        }

        //deleteObjects(AttributeScheme, *tracker.fetchRemovedAttributeIds())

        /*
        for (child in tracker.attributes.iterator().withIndex()) {
            save(child.value, child.index)
        }*/
    }
}