package kr.ac.snu.hcil.omnitrack.core.database.local

import android.content.Intent
import android.net.Uri
import io.realm.*
import io.realm.rx.RealmObservableFactory
import io.realm.rx.RxObservableFactory
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.ADatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.*

/**
 * Created by younghokim on 2017. 9. 25..
 */
class RealmDatabaseManager(val config: Configuration = Configuration()) {

    enum class Order { ASC, DESC }

    companion object {
        const val FIELD_OBJECT_ID = "objectId"
        const val FIELD_UPDATED_AT_LONG = "updatedAt"
        const val FIELD_SYNCHRONIZED_AT = "synchronizedAt"
        const val FIELD_REMOVED_BOOLEAN = "removed"
        const val FIELD_TIMESTAMP_LONG = "timestamp"

        const val FIELD_USER_ID = "userId"
        const val FIELD_TRACKER_ID = "trackerId"

        const val SAVE_RESULT_NEW = 1
        const val SAVE_RESULT_EDIT = 2
        const val SAVE_RESULT_FAIL = 0

        fun convertRealmEntryListToDictionary(realmList: RealmList<OTStringStringEntryDAO>): Map<String, String> {
            val table = Hashtable<String, String>()
            for (entryDAO in realmList) {
                table[entryDAO.key] = entryDAO.value
            }
            return table
        }

        fun convertDictionaryToRealmList(realm: Realm,
                                         dictionary: Map<String, String>,
                                         realmList: RealmList<OTStringStringEntryDAO>,
                                         valueConverter: ((String) -> String)? = null) {
            for (entry in dictionary) {
                val entryDao = realmList.find { it.key == entry.key }
                        ?: realm.createObject(OTStringStringEntryDAO::class.java, UUID.randomUUID().toString()).apply { realmList.add(this) }
                entryDao.key = entry.key
                entryDao.value = valueConverter?.invoke(entry.value) ?: entry.value
            }
        }
    }

    data class Configuration(
            val fileName: String = "localDatabase"
    )

    fun getRealmInstance(): Realm = Realm.getInstance(RealmConfiguration.Builder().name(config.fileName).build())
    private val observableFactory: RxObservableFactory by lazy {
        RealmObservableFactory()
    }

    private fun <R> usingRealm(func: (Realm) -> R): R {
        val realm = getRealmInstance()
        val result = func.invoke(realm)
        realm.close()
        return result
    }

    val OnItemListUpdated = BehaviorSubject.create<String>() // trackerId

    private fun getItemQueryOfTracker(tracker: OTTracker, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java).equalTo(FIELD_TRACKER_ID, tracker.objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getTrackerQueryWithId(objectId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_OBJECT_ID, objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getAttributeListQuery(trackerId: String, realm: Realm): RealmQuery<OTAttributeDAO> {
        return realm.where(OTAttributeDAO::class.java).equalTo(FIELD_TRACKER_ID, trackerId)
    }

    fun makeItemsQuery(trackerId: String?, from: Long?, to: Long?, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java)
                .run {
                    if (trackerId != null) {
                        return@run this.equalTo(FIELD_TRACKER_ID, trackerId)
                    } else return@run this
                }
                .run {
                    if (from != null && to != null) {
                        return@run this.between(FIELD_TIMESTAMP_LONG, from, to)
                    } else if (from != null) {
                        return@run this.greaterThanOrEqualTo(FIELD_TIMESTAMP_LONG, from)
                    } else if (to != null) {
                        return@run this.lessThan(FIELD_TIMESTAMP_LONG, to)
                    } else
                        return@run this
                }
    }

    fun makeItemsQueryOfToday(trackerId: String?, realm: Realm): RealmQuery<OTItemDAO> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val first = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val second = cal.timeInMillis

        return makeItemsQuery(trackerId, first, second, realm)
    }

    fun getDirtyItemsToSync(): Single<List<OTItemPOJO>> {
        println("get items of Realm local.")
        return Single.just(
                getRealmInstance().where(OTItemDAO::class.java).equalTo(FIELD_SYNCHRONIZED_AT, null as Long?).findAll().map { itemDao ->
                    val itemPojo = OTItemPOJO()
                    RealmItemHelper.applyDaoToPojo(itemDao, itemPojo)
                    itemPojo
                })
    }

    fun setItemSynchronizationFlags(idTimestampPair: List<SyncResultEntry>): Single<Boolean> {
        return setSynchronizationFlagsImpl(OTItemDAO::class.java, idTimestampPair)
    }

    fun applyServerItemsToSync(itemList: List<OTItemPOJO>): Single<Boolean> {
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

    fun saveTrigger(trigger: OTTrigger, userId: String, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun findTriggersOfUser(user: OTUser): Observable<List<OTTrigger>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getTrigger(user: OTUser, key: String): Observable<OTTrigger> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun removeTrigger(trigger: OTTrigger) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getItemBuilderOfTracker(trackerId: String, holderType: Int, realm: Realm): Single<Nullable<OTPendingItemBuilderDAO>> {
        return realm.where(OTPendingItemBuilderDAO::class.java).equalTo("tracker.objectId", trackerId).equalTo("holderType", holderType).findFirstAsync().asObservable<OTPendingItemBuilderDAO>()
                .skipWhile { it -> it.isLoaded == false }.map {
            if (it.isValid) {
                Nullable(it)
            } else Nullable<OTPendingItemBuilderDAO>(null)
        }.first().toSingle()
    }


    fun findTrackersOfUser(userId: String, realm: Realm): RealmResults<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId)
                .findAllAsync()
    }

    fun removeTracker(dao: OTTrackerDAO) {
        if (!dao.removed) {

            (dao.attributes + dao.removedAttributes).forEach { attrDao ->
                if (attrDao.isManaged) {
                    attrDao.deleteFromRealm()
                }
            }

            dao.removed = true
            dao.synchronizedAt = null
            dao.updatedAt = System.currentTimeMillis()
        }
    }

    fun removeTracker(objectId: String, realm: Realm) {
        val dao = getTrackerQueryWithId(objectId, realm).findFirst()
        if (dao != null) {
            removeTracker(dao)
        }
    }

    fun removeAttribute(dao: OTAttributeDAO, tracker: OTTrackerDAO, realm: Realm) {
        if (!realm.isInTransaction) {
            realm.executeTransaction {
                tracker.removedAttributes.add(dao)
            }
        } else {
            tracker.removedAttributes.add(dao)
        }
    }


    fun removeTracker(tracker: OTTracker, formerOwner: OTUser, archive: Boolean) {
        val realm = getRealmInstance()
        val dao = getTrackerQueryWithId(tracker.objectId, realm).findFirst()
        if (dao != null) {
            realm.executeTransaction {

                if (dao.removed != true) {

                    dao.attributes.forEach { attrDao ->
                        attrDao.objectId?.let {
                            removeAttribute(tracker.objectId, it)
                        }
                    }

                    dao.removed = true
                    dao.synchronizedAt = null
                    dao.updatedAt = System.currentTimeMillis()
                    //TODO set synchronization flag
                }
            }
        }
    }

    fun removeAttribute(trackerId: String, objectId: String) {
        val realm = getRealmInstance()
        val dao = realm.where(OTAttributeDAO::class.java).equalTo("trackerId", trackerId).equalTo("objectId", objectId).findFirst()
        if (dao != null) {

            /*
            realm.executeTransaction {
                if(dao.removed != true)
                {
                    dao.removed = true
                    dao.synchronizedAt = null
                    dao.updatedAt = System.currentTimeMillis()
                }
            }*/
        }
    }

    fun getTracker(key: String): Observable<OTTrackerDAO> {
        return Observable.defer {
            val realm = getRealmInstance()
            try {
                val dao = realm.where(OTTrackerDAO::class.java)
                        .equalTo(FIELD_REMOVED_BOOLEAN, false)
                        .equalTo(FIELD_OBJECT_ID, key).findFirst()
                if (dao != null) {
                    return@defer Observable.just(dao)
                } else return@defer Observable.error<OTTrackerDAO>(Exception("No tracker with such key in database."))
            } catch (ex: Exception) {
                return@defer Observable.error<OTTrackerDAO>(ex)
            } finally {
                realm.close()
            }
        }

    }

    fun saveAttribute(trackerId: String?, attribute: OTAttribute<out Any>, position: Int) {
        val realm = getRealmInstance()
        realm.executeTransaction {
            //saveAttribute(attribute, trackerId, position, realm)
        }
    }

    fun saveTracker(tracker: OTTracker, position: Int) {
        val realm = getRealmInstance()
        realm.executeTransaction {
            val baseDao = getTrackerQueryWithId(tracker.objectId, realm).findFirst() ?:
                    realm.createObject(OTTrackerDAO::class.java, UUID.randomUUID().toString())

            //baseDao.attributeLocalKeySeed = tracker.attributeLocalKeySeed
            baseDao.name = tracker.name
            baseDao.color = tracker.color
            baseDao.isBookmarked = tracker.isOnShortcut
            baseDao.isEditable = tracker.isEditable

            if (tracker.creationFlags != null) {
                convertDictionaryToRealmList(realm, tracker.creationFlags, baseDao.creationFlags)
            }

            //Deal with removed attributes===============================
            val attributeIds = tracker.attributes.map { it.objectId }
            val removedAttributeDaos = baseDao.attributes.filter { !attributeIds.contains(it.objectId) }
            removedAttributeDaos.forEach {
                it.updatedAt = System.currentTimeMillis()
            }

            //Update attributes dao updates================================
            baseDao.attributes.clear()
            tracker.attributes.unObservedList.forEachIndexed { index, attr ->
                //saveAttribute(attr, tracker.objectId, index, realm)
            }

            baseDao.synchronizedAt = null
            baseDao.updatedAt = System.currentTimeMillis()
        }
    }

    fun getLatestSynchronizedServerTimeOf(type: ESyncDataType): Single<Long>
            = when (type) {
        ESyncDataType.ITEM -> getItemLatestSynchronizedServerTime()
        else -> Single.just(0L)
    //TODO implement other syncdatatypes
    }

    fun getItemLatestSynchronizedServerTime(): Single<Long> {
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

                if (resultCode == ADatabaseManager.SAVE_RESULT_NEW) {
                    OnItemListUpdated.onNext(tracker.objectId)
                }
            }

            return@map resultCode == SAVE_RESULT_EDIT || resultCode == SAVE_RESULT_NEW
        }
    }

    fun saveItemImpl(item: OTItem, tracker: OTTracker): Single<Int> {
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

                val realm = getRealmInstance()
                realm.executeTransaction { realm ->

                    val itemDao =
                            realm.where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, item.objectId).findFirst()
                                    ?: realm.createObject(OTItemDAO::class.java, item.objectId)
                    RealmItemHelper.applyItemToDAO(item, itemDao, realm)
                    itemDao.synchronizedAt = null
                    itemDao.updatedAt = System.currentTimeMillis()

                    println("OTItem was pushed to Realm. item count: ${getItemQueryOfTracker(tracker, realm).count()}, object Id: ${item.objectId}")
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

    fun removeItem(item: OTItem) {
        item.objectId?.let { itemId ->
            removeItem(item.trackerId, itemId)
        }
    }

    fun removeItem(trackerId: String, itemId: String) {
        if (removeItemImpl(trackerId, itemId)) {

            val intent = Intent(OTApplication.BROADCAST_ACTION_ITEM_REMOVED)

            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)

            OTApplication.app.sendBroadcast(intent)

            OnItemListUpdated.onNext(trackerId)
        } else {
            println("item remove failed.")
        }
    }

    fun removeItemImpl(trackerId: String, itemId: String): Boolean {
        val realm = getRealmInstance()
        try {
            val itemDao = realm.where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, itemId).equalTo(FIELD_REMOVED_BOOLEAN, false).findFirst()
            itemDao?.let {
                realm.executeTransaction { realm ->

                    itemDao.fieldValueEntries.forEach {
                        it.deleteFromRealm()
                    }

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

    fun loadItems(tracker: OTTracker, timeRange: TimeSpan? = null, order: Order = Order.DESC): Observable<List<OTItem>> {
        return usingRealm { realm ->
            getItemQueryOfTracker(tracker, realm)
                    .run {
                        if (timeRange != null)
                            return@run this.between(FIELD_TIMESTAMP_LONG, timeRange.from, timeRange.to)
                        else this
                    }.findAllSorted(FIELD_TIMESTAMP_LONG, when (order) { Order.ASC -> Sort.ASCENDING; Order.DESC -> Sort.DESCENDING; else -> Sort.ASCENDING
            })
                    .asObservable().map { result ->
                result.map { dao ->
                    RealmItemHelper.convertDAOToItem(dao)
                }
            }
        }
    }


    fun getItem(tracker: OTTracker, itemId: String): Observable<OTItem> {
        return Observable.defer {
            val dao = getRealmInstance().where(OTItemDAO::class.java).equalTo(FIELD_OBJECT_ID, itemId).findFirst()
            if (dao != null) {
                return@defer Observable.just(RealmItemHelper.convertDAOToItem(dao))
            } else return@defer Observable.empty<OTItem>()
        }
    }

    fun setUsedAppWidget(widgetName: String, used: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getLogCountDuring(tracker: OTTracker, from: Long, to: Long): Observable<Long> =
            Observable.just(
                    usingRealm { realm -> getItemQueryOfTracker(tracker, realm).between(FIELD_TIMESTAMP_LONG, from, to).count() }
            ).subscribeOn(Schedulers.io())

    fun getTotalItemCount(tracker: OTTracker): Observable<Pair<Long, Long>> {
        return Observable.just(
                Pair(
                        usingRealm { realm -> getItemQueryOfTracker(tracker, realm).count() },
                        System.currentTimeMillis())).subscribeOn(Schedulers.io())
    }

    fun getLastLoggingTime(tracker: OTTracker): Observable<Long?> {
        return Observable.just(
                usingRealm { realm ->
                    getItemQueryOfTracker(tracker, realm).max(FIELD_TIMESTAMP_LONG)?.toLong()
                }
        ).subscribeOn(Schedulers.io())
    }
}