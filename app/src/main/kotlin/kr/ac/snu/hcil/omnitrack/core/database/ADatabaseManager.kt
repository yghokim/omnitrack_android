package kr.ac.snu.hcil.omnitrack.core.database

import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Single

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

    abstract fun getLogCountOfDay(tracker: OTTracker): Observable<Long>
    abstract fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long, Long>>
    abstract fun getLastLoggingTime(tracker: OTTracker): Observable<Long?>

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
            }

            return@map resultCode == SAVE_RESULT_EDIT || resultCode == SAVE_RESULT_NEW
        }
    }

    protected abstract fun saveItemImpl(item: OTItem, tracker: OTTracker): Single<Int>

    abstract fun loadItems(tracker: OTTracker, timeRange: TimeSpan? = null, order: ADatabaseManager.Order = Order.DESC): Observable<List<OTItem>>
    abstract fun removeItem(item: OTItem)
    abstract fun removeItem(trackerId: String, itemId: String)
    abstract fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem>

    abstract fun checkHasDeviceId(userId: String, deviceId: String): Single<Boolean>
    abstract fun addDeviceInfoToUser(userId: String, deviceId: String): Single<OTDeviceInfo>
    abstract fun refreshInstanceIdToServerIfExists(ignoreIfStored: Boolean): Boolean
    abstract fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>
}