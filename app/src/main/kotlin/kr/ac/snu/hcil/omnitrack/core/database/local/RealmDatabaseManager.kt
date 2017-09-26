package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.Sort
import io.realm.rx.RealmObservableFactory
import io.realm.rx.RxObservableFactory
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.ADatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Single

/**
 * Created by younghokim on 2017. 9. 25..
 */
class RealmDatabaseManager(val config: Configuration = Configuration()) : ADatabaseManager() {

    data class Configuration(
            val fileName: String = "localDatabase"
    )

    private val realmInstance: Realm get() = Realm.getInstance(RealmConfiguration.Builder().name(config.fileName).build())
    private val observableFactory: RxObservableFactory by lazy {
        RealmObservableFactory()
    }

    override fun saveTrigger(trigger: OTTrigger, userId: String, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findTriggersOfUser(user: OTUser): Observable<List<OTTrigger>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTrigger(user: OTUser, key: String): Observable<OTTrigger> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeTracker(tracker: OTTracker, formerOwner: OTUser, archive: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeTrigger(trigger: OTTrigger) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAttribute(trackerId: String, objectId: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findTrackersOfUser(userId: String): Observable<List<OTTracker>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTracker(key: String): Observable<OTTracker> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveAttribute(trackerId: String, attribute: OTAttribute<out Any>, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveTracker(tracker: OTTracker, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun saveItemImpl(item: OTItem, tracker: OTTracker): Single<Int> {
        return Single.create { subscriber ->
            var result: Int = SAVE_RESULT_FAIL
            try {
                realmInstance.executeTransaction { realm ->
                    result = if (item.objectId == null) {
                        SAVE_RESULT_NEW
                    } else {
                        SAVE_RESULT_EDIT
                    }
                    realm.insertOrUpdate(RealmItemHelper.convertItemToDAO(item))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                if (!subscriber.isUnsubscribed) {
                    subscriber.onSuccess(result)
                }
            }
        }
    }

    override fun removeItem(item: OTItem) {

        val itemDao = realmInstance.where(OTItemDAO::class.java).equalTo("objectId", item.objectId).findFirst()
        realmInstance.executeTransaction {
            itemDao?.deleteFromRealm()
        }
    }

    override fun removeItem(trackerId: String, itemId: String) {
        val itemDao = realmInstance.where(OTItemDAO::class.java).equalTo("objectId", itemId).findFirst()
        realmInstance.executeTransaction {
            itemDao?.deleteFromRealm()
        }
    }

    override fun loadItems(tracker: OTTracker, timeRange: TimeSpan?, order: ADatabaseManager.Order): Observable<List<OTItem>> {
        return realmInstance.where(OTItemDAO::class.java).equalTo("trackerObjectId", tracker.objectId)
                .run {
                    if (timeRange != null)
                        return@run this.between("timestamp", timeRange.from, timeRange.to)
                    else this
                }.findAllSorted("timestamp", when (order) { Order.ASC -> Sort.ASCENDING; Order.DESC -> Sort.DESCENDING
        })
                .asObservable().map { result ->
            result.map { dao ->
                RealmItemHelper.convertDAOToItem(dao)
            }
        }
    }


    override fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem> {
        return Observable.defer {
            val dao = realmInstance.where(OTItemDAO::class.java).equalTo("objectId", itemId).findFirst()
            if (dao != null) {
                return@defer Observable.just(RealmItemHelper.convertDAOToItem(dao))
            } else return@defer Observable.empty<OTItem>()
        }
    }

    override fun setUsedAppWidget(widgetName: String, used: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Observable<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLogCountOfDay(tracker: OTTracker): Observable<Long> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long, Long>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLastLoggingTime(tracker: OTTracker): Observable<Long?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkHasDeviceId(userId: String, deviceId: String): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addDeviceInfoToUser(userId: String, deviceId: String): Single<OTDeviceInfo> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun refreshInstanceIdToServerIfExists(ignoreIfStored: Boolean): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeDeviceInfo(userId: String, deviceId: String): Single<Boolean> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}