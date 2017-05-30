package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.CompositeSubscription
import java.lang.ref.WeakReference

/**
 * Created by younghokim on 2017. 5. 5..
 */
class ItemCountTracer(tracker: OTTracker) {

    companion object {
        private val preferences: SharedPreferences by lazy {
            OTApplication.app.getSharedPreferences("ItemCountTracerCache", Context.MODE_PRIVATE)
        }

        @Synchronized() fun setCounterCache(trackerId: String, count: Long, timestamp: Long): Boolean {
            val updateTimestamp = preferences.getLong("timestamp_${trackerId}", -1)
            if (updateTimestamp < timestamp) {
                val editor = preferences.edit()
                editor.putLong("timestamp_${trackerId}", timestamp)
                editor.putLong(trackerId, count)
                editor.apply()
                return true
            } else return false
        }


        fun getCachedCount(trackerId: String): Long {
            return preferences.getLong(trackerId, 0)
        }
    }

    var isRegistered: Boolean = false
        private set


    private val trackerWeak: WeakReference<OTTracker> = WeakReference(tracker)

    val itemCountObservable = SerializedSubject(PublishSubject.create<Long>())

    private val itemCountDatabaseSubscription = CompositeSubscription()

    private var preferenceChangedListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private @Synchronized() fun setCounterCache(count: Long, timestamp: Long) {
        val tracker = trackerWeak.get()
        if (tracker != null) {
            if (setCounterCache(tracker.objectId, count, timestamp)) {
                itemCountObservable.onNext(count)
            }
        }
    }

    fun register() {
        val tracker = trackerWeak.get()
        if (tracker != null) {
            itemCountDatabaseSubscription.add(DatabaseManager.getTotalItemCount(tracker).subscribe {
                result ->
                setCounterCache(result.first, result.second)
            })

            //itemListDatabaseRef?.addChildEventListener(itemListChangedEventListener)
            preferenceChangedListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
                override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
                    if (key == trackerWeak.get()?.objectId) {
                        itemCountObservable.onNext(sharedPreferences.getLong(key, -1))
                    }
                }

            }
            preferences.registerOnSharedPreferenceChangeListener(preferenceChangedListener)

            isRegistered = true
        }

    }

    fun unregister() {
        itemCountDatabaseSubscription.clear()
        if (preferenceChangedListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangedListener)
        }
        //itemListDatabaseRef?.removeEventListener(itemListChangedEventListener)
        isRegistered = false
    }

    fun notifyConnected() {

        val tracker = trackerWeak.get()
        if (tracker != null) {
            itemCountObservable.onNext(getCachedCount(tracker.objectId))
        }
    }
}