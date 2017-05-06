package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.SerialSubscription
import rx.subscriptions.Subscriptions
import java.lang.ref.WeakReference

/**
 * Created by younghokim on 2017. 5. 5..
 */
class ItemCountTracer(tracker: OTTracker) {

    companion object {
        private val preferences: SharedPreferences by lazy {
            OTApplication.app.getSharedPreferences("ItemCountTracerCache", Context.MODE_PRIVATE)
        }
    }

    var isRegistered: Boolean = false
        private set


    private val trackerWeak: WeakReference<OTTracker> = WeakReference(tracker)

    val itemCountObservable = SerializedSubject(PublishSubject.create<Long>())

    private val itemCountDatabaseSubscription: SerialSubscription = SerialSubscription()

    private val itemListDatabaseRef: Query? = DatabaseManager.getItemListOfTrackerChild(tracker.objectId)?.limitToLast(1)

    private val itemListChangedEventListener = object : ChildEventListener {
        override fun onCancelled(p0: DatabaseError?) {
        }

        override fun onChildMoved(p0: DataSnapshot?, p1: String?) {
        }

        override fun onChildChanged(p0: DataSnapshot?, p1: String?) {
        }

        override fun onChildAdded(snapshot: DataSnapshot, p1: String?) {
            val timestamp = snapshot.child("timestamp").getValue().toString().toLong()
            val newCount = (getCachedCount() ?: 0) + 1
            setCounterCache(newCount, timestamp)
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val timestamp = snapshot.child("timestamp").getValue().toString().toLong()
            val newCount = (getCachedCount() ?: 0) - 1
            setCounterCache(newCount, timestamp)
        }
    }

    private fun getCachedCount(): Long? {
        val trackerId = trackerWeak.get()?.objectId
        if (trackerId != null) {
            return preferences.getLong(trackerId, 0)
        } else return null
    }

    private @Synchronized() fun setCounterCache(count: Long, timestamp: Long) {
        val tracker = trackerWeak.get()
        if (tracker != null) {

            val updateTimestamp = preferences.getLong("timestamp_${tracker.objectId}", -1)
            if (updateTimestamp < timestamp) {
                val editor = preferences.edit()
                editor.putLong(tracker.objectId, count)
                editor.putLong("timestamp_${tracker.objectId}", timestamp)
                editor.apply()

                itemCountObservable.onNext(count)
            }
        }
    }

    fun register() {
        val tracker = trackerWeak.get()
        if (tracker != null) {
            itemCountDatabaseSubscription.set(DatabaseManager.getTotalItemCount(tracker).subscribe {
                count ->
                setCounterCache(count, System.currentTimeMillis())
            })

            itemListDatabaseRef?.addChildEventListener(itemListChangedEventListener)

            isRegistered = true
        }

    }

    fun unregister() {
        itemCountDatabaseSubscription.set(Subscriptions.empty())
        itemListDatabaseRef?.removeEventListener(itemListChangedEventListener)
        isRegistered = false
    }

    fun notifyConnected() {
        itemCountObservable.onNext(getCachedCount())
    }
}