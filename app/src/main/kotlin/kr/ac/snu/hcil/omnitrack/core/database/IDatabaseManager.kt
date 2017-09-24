package kr.ac.snu.hcil.omnitrack.core.database

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
interface IDatabaseManager {
    enum class Order { ASC, DESC }

    fun saveTrigger(trigger: OTTrigger, userId: String, position: Int)
    fun findTriggersOfUser(user: OTUser): Observable<List<OTTrigger>>
    fun getTrigger(user: OTUser, key: String): Observable<OTTrigger>
    fun removeTracker(tracker: OTTracker, formerOwner: OTUser, archive: Boolean = true)
    fun removeTrigger(trigger: OTTrigger)
    fun removeAttribute(trackerId: String, objectId: String)

    fun findTrackersOfUser(userId: String): Observable<List<OTTracker>>
    fun getTracker(key: String): Observable<OTTracker>
    fun saveAttribute(trackerId: String, attribute: OTAttribute<out Any>, position: Int)
    fun saveTracker(tracker: OTTracker, position: Int)
    fun removeItem(item: OTItem)
    fun removeItem(trackerId: String, itemId: String)
    fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem>
    fun setUsedAppWidget(widgetName: String, used: Boolean)
    fun loadItems(tracker: OTTracker, timeRange: TimeSpan? = null, order: IDatabaseManager.Order = Order.DESC): Observable<List<OTItem>>
    fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Observable<Long>
    fun getLogCountOfDay(tracker: OTTracker): Observable<Long>
    fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long, Long>>
    fun getLastLoggingTime(tracker: OTTracker): Observable<Long?>
    fun saveItem(item: OTItem, tracker: OTTracker, notifyIntent: Boolean = true, finished: ((Boolean) -> Unit)? = null)
    fun checkHasDeviceId(userId: String, deviceId: String): Single<Boolean>
    fun addDeviceInfoToUser(userId: String, deviceId: String): Single<OTDeviceInfo>
    fun refreshInstanceIdToServerIfExists(ignoreIfStored: Boolean): Boolean
    fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean>
}