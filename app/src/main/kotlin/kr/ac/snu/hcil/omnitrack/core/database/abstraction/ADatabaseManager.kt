package kr.ac.snu.hcil.omnitrack.core.database.abstraction

import android.content.Intent
import android.net.Uri
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Single
import rx.subjects.BehaviorSubject
import java.util.*

/**
 * Created by Young-Ho on 9/24/2017.
 */
abstract class ADatabaseManager {
    enum class Order { ASC, DESC }

    companion object {

        const val SAVE_RESULT_NEW = 1
        const val SAVE_RESULT_EDIT = 2
        const val SAVE_RESULT_FAIL = 0
    }

    val OnItemListUpdated = BehaviorSubject.create<String>() // trackerId

    abstract fun saveTrigger(trigger: OTTrigger, userId: String, position: Int)
    abstract fun findTriggersOfUser(user: OTUser): Observable<List<OTTrigger>>
    abstract fun getTrigger(user: OTUser, key: String): Observable<OTTrigger>
    abstract fun removeTracker(tracker: OTTracker, formerOwner: OTUser, archive: Boolean = true)
    abstract fun removeTrigger(trigger: OTTrigger)
    abstract fun removeAttribute(trackerId: String, objectId: String)

    abstract fun findTrackersOfUser(userId: String): Observable<List<OTTracker>>
    abstract fun getTracker(key: String): Observable<OTTracker>
    abstract fun saveAttribute(trackerId: String, attribute: OTAttribute<out Any>, position: Int)
    abstract fun saveTracker(tracker: OTTracker, position: Int)

    abstract fun setUsedAppWidget(widgetName: String, used: Boolean)

    //Item Summaries
    abstract fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Observable<Long>

    fun getLogCountOfDay(tracker: OTTracker): Observable<Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val first = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val second = cal.timeInMillis - 20

        return getLogCountDuring(tracker, first, second)
    }

    abstract fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long/*count*/, Long/*timestamp*/>>
    abstract fun getLastLoggingTime(tracker: OTTracker): Observable<Long?>

    protected fun handleBinaryUpload(itemId: String, item: OTItem, tracker: OTTracker) {
        tracker.attributes.unObservedList.forEach {
            val value = item.getValueOf(it)
            if (value is SynchronizedUri && value.localUri != Uri.EMPTY) {
                println("upload Synchronized Uri file to server...")
                value.setSynchronized(OTApplication.app.binaryUploadServiceController.makeFilePath(itemId, tracker.objectId, tracker.owner!!.objectId, value.localUri.lastPathSegment))

                OTApplication.app.startService(
                        OTApplication.app.binaryUploadServiceController.makeUploadServiceIntent(value, itemId, tracker.objectId, tracker.owner!!.objectId)
                )
            }
        }
    }

    //Items
    fun saveItem(item: OTItem, tracker: OTTracker, notifyIntent: Boolean = true): Single<Boolean> {
        return saveItemImpl(item, tracker).map { resultCode ->
            if (notifyIntent && resultCode != SAVE_RESULT_FAIL) {

                val intent = Intent(when (resultCode) {
                    SAVE_RESULT_NEW -> OTApplication.BROADCAST_ACTION_ITEM_ADDED
                    SAVE_RESULT_EDIT -> OTApplication.BROADCAST_ACTION_ITEM_EDITED
                    else -> throw IllegalArgumentException("")
                })

                intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId)

                OTApplication.app.sendBroadcast(intent)

                if (resultCode == SAVE_RESULT_NEW) {
                    OnItemListUpdated.onNext(tracker.objectId)
                }
                }

            return@map resultCode == SAVE_RESULT_EDIT || resultCode == SAVE_RESULT_NEW
        }
    }

    protected abstract fun saveItemImpl(item: OTItem, tracker: OTTracker): Single<Int>

    abstract fun loadItems(tracker: OTTracker, timeRange: TimeSpan? = null, order: Order = Order.DESC): Observable<List<OTItem>>

    fun removeItem(item: OTItem) {
        item.objectId?.let { itemId ->
            removeItem(item.trackerObjectId, itemId)
        }
    }

    fun removeItem(trackerId: String, itemId: String) {
        if (removeItemImpl(trackerId, itemId)) {

            val intent = Intent(OTApplication.BROADCAST_ACTION_ITEM_REMOVED)

            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)

            OTApplication.app.sendBroadcast(intent)

            OnItemListUpdated.onNext(trackerId)
        }
    }


    abstract fun removeItemImpl(trackerId: String, itemId: String): Boolean

    abstract fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem>

    abstract fun checkHasDeviceId(userId: String, deviceId: String): Single<Boolean>
    abstract fun addDeviceInfoToUser(userId: String, deviceId: String): Single<OTDeviceInfo>
    abstract fun refreshInstanceIdToServerIfExists(ignoreIfStored: Boolean): Boolean
    abstract fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>
}