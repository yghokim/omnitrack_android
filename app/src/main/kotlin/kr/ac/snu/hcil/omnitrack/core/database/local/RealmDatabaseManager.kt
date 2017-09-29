package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.*
import io.realm.rx.RealmObservableFactory
import io.realm.rx.RxObservableFactory
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.OTDeviceInfo
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.ADatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by younghokim on 2017. 9. 25..
 */
class RealmDatabaseManager(val config: Configuration = Configuration()) : ADatabaseManager() {

    companion object {
        const val FIELD_OBJECT_ID = "objectId"
        const val FIELD_UPDATED_AT_LONG = "updatedAt"
        const val FIELD_SYNCHRONIZED_AT = "synchronizedAt"
        const val FIELD_REMOVED_BOOLEAN = "removed"
        const val FIELD_TIMESTAMP_LONG = "timestamp"
        const val FIELD_TRACKER_ID = "trackerObjectId"
    }

    data class Configuration(
            val fileName: String = "localDatabase"
    )

    private fun getRealmInstance(): Realm = Realm.getInstance(RealmConfiguration.Builder().name(config.fileName).build())
    private val observableFactory: RxObservableFactory by lazy {
        RealmObservableFactory()
    }

    private fun getItemQueryOfTracker(tracker: OTTracker): RealmQuery<OTItemDAO> {
        return getRealmInstance().where(OTItemDAO::class.java).equalTo(FIELD_TRACKER_ID, tracker.objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    override fun getDirtyItemsToSync(): Single<List<OTItemPOJO>> {
        println("get items of Realm local.")
        return Single.just(
                getRealmInstance().where(OTItemDAO::class.java).equalTo(FIELD_SYNCHRONIZED_AT, null as Long?).findAll().map { itemDao ->
                    val itemPojo = OTItemPOJO()
                    RealmItemHelper.applyDaoToPojo(itemDao, itemPojo)
                    itemPojo
                })
    }

    override fun setItemSynchronizationFlags(idTimestampPair: List<SyncResultEntry>): Single<Boolean> {
        return setSynchronizationFlagsImpl(OTItemDAO::class.java, idTimestampPair)
    }

    override fun applyServerItemsToSync(itemList: List<OTItemPOJO>): Single<Boolean> {
        return Single.defer {
            val realm = getRealmInstance()
            try {
                for (serverPojo in itemList) {
                    val match = realm.where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, serverPojo.objectId).findFirst()
                    if (match == null) {
                        if (!serverPojo.removed) {
                            //insert
                            println("synchronization: server row not matched and is not a removed row. append new in local db.")

                            realm.executeTransaction { realm ->
                                val dao = realm.createObject(OTItemDAO::class.java, serverPojo.objectId)
                                RealmItemHelper.applyPojoToDao(serverPojo, dao, realm)
                            }
                        }
                    } else if (match.synchronizedAt == null) {
                        //found matched row, but it is dirty. Conflict!
                        //late timestamp win policy
                        println("conflict")
                        if (match.timestamp > serverPojo.timestamp) {
                            //client win
                            println("client win")
                        } else {
                            //server win
                            println("server win")
                            realm.executeTransaction {
                                RealmItemHelper.applyPojoToDao(serverPojo, match, realm)
                            }
                        }
                    } else {
                        //update
                        realm.executeTransaction {
                            if (serverPojo.removed) {
                                match.deleteFromRealm()
                            } else {
                                RealmItemHelper.applyPojoToDao(serverPojo, match, realm)
                            }
                        }
                    }
                }

                realm.close()
                return@defer Single.just(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@defer Single.just(false)
            } finally {
                realm.close()
            }
        }
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


    override fun getItemLatestSynchronizedServerTime(): Single<Long> {
        return getLatestSynchronizedServerTimeImpl(OTItemDAO::class.java)
    }

    private fun setSynchronizationFlagsImpl(daoClass: Class<out RealmObject>, idTimestampPair: List<SyncResultEntry>): Single<Boolean> {
        return Single.defer {
            try {
                getRealmInstance().let {

                    it.executeTransaction { realm ->
                        for (pair in idTimestampPair) {
                            val row = realm.where(daoClass).equalTo(FIELD_OBJECT_ID, pair.id).findFirst()
                            if (row != null) {
                                if (row is OTItemDAO) {
                                    row.synchronizedAt = pair.synchronizedAt
                                }
                                println("set synchronization timestamp to ${pair.synchronizedAt}")
                            }
                        }
                    }
                    it.close()
                }
                return@defer Single.just(true)
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@defer Single.just(false)
            }
        }
    }

    private fun getLatestSynchronizedServerTimeImpl(daoClass: Class<out RealmObject>): Single<Long> {
        return Single.just(getRealmInstance().where(daoClass).max(FIELD_SYNCHRONIZED_AT)?.toLong() ?: 0)
    }

    override fun saveItemImpl(item: OTItem, tracker: OTTracker): Single<Int> {
        return Single.create { subscriber ->
            var result: Int = SAVE_RESULT_FAIL
            try {
                result = if (item.objectId == null) {
                    SAVE_RESULT_NEW
                } else {
                    SAVE_RESULT_EDIT
                }

                //if itemId is null, set new Id.
                if (item.objectId == null) {
                    val newItemId = tracker.objectId + UUID.randomUUID().toString()
                    item.objectId = newItemId
                }

                handleBinaryUpload(item.objectId!!, item, tracker)

                getRealmInstance().executeTransaction { realm ->
                    realm.insertOrUpdate(RealmItemHelper.convertItemToDAO(item).apply {
                        this.synchronizedAt = null
                        this.updatedAt = System.currentTimeMillis()
                    })

                    println("OTItem was pushed to Realm. item count: ${getItemQueryOfTracker(tracker).count()}, object Id: ${item.objectId}")
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

    override fun removeItemImpl(trackerId: String, itemId: String): Boolean {
        val realm = getRealmInstance()
        try {
            val itemDao = realm.where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, itemId).findFirst()
            itemDao?.let {
                realm.executeTransaction { realm ->
                    itemDao.removed = true
                    itemDao.synchronizedAt = null
                    itemDao.updatedAt = System.currentTimeMillis()
                }
                return true
            } ?: return false
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        } finally {
            realm.close()
        }
    }

    override fun loadItems(tracker: OTTracker, timeRange: TimeSpan?, order: ADatabaseManager.Order): Observable<List<OTItem>> {
        return getItemQueryOfTracker(tracker)
                .run {
                    if (timeRange != null)
                        return@run this.between(FIELD_TIMESTAMP_LONG, timeRange.from, timeRange.to)
                    else this
                }.findAllSorted(FIELD_TIMESTAMP_LONG, when (order) { Order.ASC -> Sort.ASCENDING; Order.DESC -> Sort.DESCENDING
        })
                .asObservable().map { result ->
            result.map { dao ->
                RealmItemHelper.convertDAOToItem(dao)
            }
        }
    }


    override fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem> {
        return Observable.defer {
            val dao = getRealmInstance().where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, itemId).findFirst()
            if (dao != null) {
                return@defer Observable.just(RealmItemHelper.convertDAOToItem(dao))
            } else return@defer Observable.empty<OTItem>()
        }
    }

    override fun setUsedAppWidget(widgetName: String, used: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Observable<Long> =
            Observable.just(getItemQueryOfTracker(tracker).between(FIELD_TIMESTAMP_LONG, from, to).count()).subscribeOn(Schedulers.io())


    override fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long, Long>> {
        return Observable.just(
                Pair(
                        getItemQueryOfTracker(tracker).count(),
                        System.currentTimeMillis())).subscribeOn(Schedulers.io())
    }

    override fun getLastLoggingTime(tracker: OTTracker): Observable<Long?> {
        return Observable.just(
                getItemQueryOfTracker(tracker).max(FIELD_TIMESTAMP_LONG)?.toLong()
        ).subscribeOn(Schedulers.io())
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