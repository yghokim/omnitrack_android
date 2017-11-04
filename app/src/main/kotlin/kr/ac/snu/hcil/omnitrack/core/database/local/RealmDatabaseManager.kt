package kr.ac.snu.hcil.omnitrack.core.database.local

import android.content.Intent
import android.net.Uri
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.realm.*
import io.realm.rx.RealmObservableFactory
import io.realm.rx.RxObservableFactory
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncResultEntry
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.net.ABinaryUploadService
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 9. 25..
 */
@Singleton
class RealmDatabaseManager @Inject constructor(private val config: Configuration, private val authManager: OTAuthManager, private val binaryUploadServiceController: ABinaryUploadService.ABinaryUploadServiceController)
    :ISynchronizationClientSideAPI{

    companion object {
        const val FIELD_OBJECT_ID = "objectId"
        const val FIELD_UPDATED_AT_LONG = "updatedAt"
        const val FIELD_USER_CREATED_AT = "userCreatedAt"
        const val FIELD_SYNCHRONIZED_AT = "synchronizedAt"
        const val FIELD_REMOVED_BOOLEAN = "removed"
        const val FIELD_TIMESTAMP_LONG = "timestamp"

        const val FIELD_NAME = "name"
        const val FIELD_POSITION = "position"

        const val FIELD_USER_ID = "userId"
        const val FIELD_TRACKER_ID = "trackerId"

        const val SAVE_RESULT_NEW = 1
        const val SAVE_RESULT_EDIT = 2
        const val SAVE_RESULT_FAIL = 0
    }

    data class Configuration(
            val fileName: String = "localDatabase"
    )

    fun makeNewRealmInstance(): Realm = Realm.getInstance(RealmConfiguration.Builder().name(config.fileName).build())
    private val observableFactory: RxObservableFactory by lazy {
        RealmObservableFactory()
    }

    private fun <R> usingRealm(func: (Realm) -> R): R {
        val realm = makeNewRealmInstance()
        val result = func.invoke(realm)
        realm.close()
        return result
    }

    val OnItemListUpdated = PublishSubject.create<String>() // trackerId

    private fun getItemQueryOfTracker(tracker: OTTracker, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java).equalTo(FIELD_TRACKER_ID, tracker.objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getTrackerQueryWithId(objectId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_OBJECT_ID, objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun makeBookmarkedTrackersObservable(userId: String, realm: Realm): Flowable<RealmResults<OTTrackerDAO>> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId).equalTo("isBookmarked", true).findAllSortedAsync("position", Sort.ASCENDING)
                .asFlowable()
    }

    fun makeShortcutPanelRefreshObservable(userId: String, realm: Realm): Flowable<RealmResults<OTTrackerDAO>> {
        return makeBookmarkedTrackersObservable(userId, realm).filter { it.isValid && it.isLoaded }
                .doAfterNext { list ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(list, OTApp.instance)
                }
    }

    /*
    fun makeBookmarkedBroadcastObservable(userId: String, realm: Realm): Flowable<Intent> {
        return makeBookmarkedTrackersObservable(userId, realm).filter { it.isValid && it.isLoaded }.map { snapshot ->
            Intent(OTShortcutPanelManager.ACTION_BOOKMARKED_TRACKERS_CHANGED)
                    .apply {
                        putExtra(OTShortcutPanelManager.INTENT_EXTRA_CURRENT_BOOKMARKED_SNAPSHOT, snapshot.map { it.objectId }.toTypedArray())
                    }
        }.doOnNext {
                intent->
        }
    }*/

    fun getAttributeListQuery(trackerId: String, realm: Realm): RealmQuery<OTAttributeDAO> {
        return realm.where(OTAttributeDAO::class.java).equalTo(FIELD_TRACKER_ID, trackerId)
    }

    fun getUnManagedTrackerDao(trackerId: String?, realm: Realm?): OTTrackerDAO? {
        if (trackerId == null) {
            return null
        }

        val realmInstance = if (realm != null) {
            realm
        } else makeNewRealmInstance()

        val trackerDao = getTrackerQueryWithId(trackerId, realmInstance).findFirst()
        if (trackerDao != null) {
            val unmanaged = realmInstance.copyFromRealm(trackerDao)

            if (realm == null) {
                realmInstance.close()
            }

            return unmanaged
        } else {
            if (realm == null) {
                realmInstance.close()
            }
            return null
        }
    }

    fun makeItemsQuery(trackerId: String?, scope: TimeSpan, realm: Realm): RealmQuery<OTItemDAO> {
        return makeItemsQuery(trackerId, scope.from, scope.to, realm)
    }

    fun makeItemsQuery(trackerId: String?, from: Long?, to: Long?, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java).equalTo("removed", false)
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

    fun makeSingleItemQuery(itemId: String, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java).equalTo("objectId", itemId)
    }


    fun getItemBuilderQuery(trackerId: String, holderType: Int, realm: Realm): RealmQuery<OTItemBuilderDAO> {
        return realm.where(OTItemBuilderDAO::class.java).equalTo("tracker.objectId", trackerId).equalTo("holderType", holderType)
    }

    fun makeTriggersOfUserQuery(userId: String, realm: Realm): RealmQuery<OTTriggerDAO> {
        return realm.where(OTTriggerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId)
    }

    fun makeTrackersOfUserQuery(userId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId)
    }

    fun removeTracker(dao: OTTrackerDAO, permanently: Boolean = false, realm: Realm) {
        if (!permanently) {
            if (!dao.removed) {
                realm.executeTransactionIfNotIn {
                    dao.removed = true
                    dao.synchronizedAt = null
                    dao.updatedAt = System.currentTimeMillis()
                }
            }
        } else {
            realm.executeTransactionIfNotIn {
                (dao.attributes + dao.removedAttributes).forEach { removeAttributeImpl(it, realm) }
                dao.attributes.clear()
                dao.removedAttributes.clear()
                dao.deleteFromRealm()
            }
        }
    }

    private fun removeAttributeImpl(dao: OTAttributeDAO, realm: Realm) {
        if (dao.isManaged) {
            realm.executeTransactionIfNotIn {
                dao.properties.deleteAllFromRealm()
                dao.deleteFromRealm()
            }
        }
    }

    fun removeTracker(tracker: OTTracker, formerOwner: OTUser, archive: Boolean) {
        val realm = makeNewRealmInstance()
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
                }
            }
        }
    }

    fun saveTrigger(dao: OTTriggerDAO, realm: Realm) {
        if (realm.isInTransaction) {
            realm.copyToRealmOrUpdate(dao)
        } else {
            realm.executeTransactionAsync({ realm ->
                if (!dao.isManaged && dao.trackers.isNotEmpty()) {
                    val trackers = realm.where(OTTrackerDAO::class.java).`in`("objectId", dao.trackers.map { it.objectId }.toTypedArray()).findAll()
                    dao.trackers.clear()
                    dao.trackers.addAll(trackers)
                }
                realm.copyToRealmOrUpdate(dao)
            }, {}, { err -> err.printStackTrace() })
        }
    }

    fun removeTrigger(dao: OTTriggerDAO, realm: Realm) {
        fun process(realm: Realm) {
            //TODO handle detach trigger from system

            dao.synchronizedAt = null
            dao.updatedAt = System.currentTimeMillis()
            dao.removed = true
            if (!dao.isManaged) {
                realm.copyToRealmOrUpdate(dao)
            }
        }

        if (realm.isInTransaction) {
            process(realm)
        } else {
            realm.executeTransaction { r ->
                process(r)
            }
        }
    }

    fun removeAttribute(trackerId: String, objectId: String) {
        val realm = makeNewRealmInstance()
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
            val realm = makeNewRealmInstance()
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

    //Items
    fun saveItemObservable(item: OTItemDAO, notifyIntent: Boolean = true, localIdsToIgnore: Array<String>? = null, realm: Realm): Single<Pair<Int, String?>> {
        return saveItemImpl(item, localIdsToIgnore, realm).doOnSuccess { resultPair ->
            if (notifyIntent && resultPair.first != SAVE_RESULT_FAIL) {

                val intent = Intent(when (resultPair.first) {
                    SAVE_RESULT_NEW -> OTApp.BROADCAST_ACTION_ITEM_ADDED
                    SAVE_RESULT_EDIT -> OTApp.BROADCAST_ACTION_ITEM_EDITED
                    else -> throw IllegalArgumentException("")
                })

                intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, item.trackerId)
                intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, item.objectId)

                OTApp.instance.sendBroadcast(intent)

                if (resultPair.first == SAVE_RESULT_NEW) {
                    OnItemListUpdated.onNext(item.trackerId!!)
                }
            }
        }
    }

    fun saveItemImpl(item: OTItemDAO, localIdsToIgnore: Array<String>? = null, realm: Realm): Single<Pair<Int, String?>> {
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
                    val newItemId = UUID.randomUUID().toString()
                    item.objectId = newItemId
                }

                if (realm.isInTransaction) {
                    handleBinaryUpload(item.objectId!!, item, localIdsToIgnore)
                    realm.copyToRealmOrUpdate(item)
                } else {
                    realm.executeTransaction { realm ->
                        handleBinaryUpload(item.objectId!!, item, localIdsToIgnore)
                        realm.copyToRealmOrUpdate(item)
                    }
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                if (!subscriber.isDisposed) {
                    subscriber.onSuccess(Pair(result, item.objectId))
                }
            }
        }
    }

    protected fun handleBinaryUpload(itemId: String, item: OTItemDAO, localIdsToIgnore: Array<String>?) {

        item.fieldValueEntries.filter { localIdsToIgnore?.contains(it.key) != true }.forEach { entry ->
            val value = entry.value?.let { TypeStringSerializationHelper.deserialize(it) }
            if (value != null) {
                if (value is SynchronizedUri && value.localUri != Uri.EMPTY) {
                    println("upload Synchronized Uri file to server...")
                    value.setSynchronized(binaryUploadServiceController.makeFilePath(itemId, item.trackerId!!, authManager.userId!!, value.localUri.lastPathSegment))
                    entry.value = TypeStringSerializationHelper.serialize(value)
                    OTApp.instance.startService(
                            binaryUploadServiceController.makeUploadServiceIntent(value, itemId, item.trackerId!!, authManager.userId!!)
                    )
                }
            }
        }
    }

    fun removeItem(itemDao: OTItemDAO, permanently: Boolean = false, realm: Realm) {
        val trackerId = itemDao.trackerId
        if (removeItemImpl(itemDao, permanently, realm)) {

            val intent = Intent(OTApp.BROADCAST_ACTION_ITEM_REMOVED)

            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)

            OTApp.instance.sendBroadcast(intent)

            OnItemListUpdated.onNext(trackerId!!)
        } else {
            println("item remove failed.")
        }
    }

    private fun removeItemImpl(itemDao: OTItemDAO, permanently: Boolean = false, realm: Realm): Boolean {
        try {
            if (!permanently) {
                realm.executeTransactionIfNotIn {
                    itemDao.removed = true
                    itemDao.synchronizedAt = null
                    itemDao.updatedAt = System.currentTimeMillis()
                }
            } else {
                realm.executeTransactionIfNotIn {
                    itemDao.fieldValueEntries.deleteAllFromRealm()
                    itemDao.deleteFromRealm()
                }
            }

            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    fun setUsedAppWidget(widgetName: String, used: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    //Item Sync APIs==========================================

    override fun getLatestSynchronizedServerTimeOf(type: ESyncDataType): Single<Long>
            = when (type) {
        ESyncDataType.ITEM -> getLatestSynchronizedServerTimeImpl(OTItemDAO::class.java)
        ESyncDataType.TRIGGER -> getLatestSynchronizedServerTimeImpl(OTTriggerDAO::class.java)
        ESyncDataType.TRACKER -> getLatestSynchronizedServerTimeImpl(OTTrackerDAO::class.java)
        else -> Single.just(0L)
    //TODO implement user type
    }

    private fun getLatestSynchronizedServerTimeImpl(daoClass: Class<out RealmObject>): Single<Long> {
        return Single.just(makeNewRealmInstance().where(daoClass).max(FIELD_SYNCHRONIZED_AT)?.toLong() ?: 0)
    }

    private fun <T: RealmObject> setSynchronizationFlagsImpl(daoClass: Class<T>, idTimestampPair: List<SyncResultEntry>, setFlagFunc: (T,Long)->Unit): Completable {
        return Completable.defer {
            try {
                makeNewRealmInstance().let {

                    it.executeTransaction { realm ->
                        for (pair in idTimestampPair) {
                            val row = realm.where(daoClass).equalTo(FIELD_OBJECT_ID, pair.id).findFirst()
                            if (row != null) {
                                setFlagFunc.invoke(row, pair.synchronizedAt)
                                println("set synchronization timestamp to ${pair.synchronizedAt}")
                            }
                        }
                    }
                    it.close()
                }
                return@defer Completable.complete()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@defer Completable.error(Exception(""))
            }
        }
    }

    override fun setTableSynchronizationFlags(type: ESyncDataType, idTimestampPair: List<SyncResultEntry>): Completable {
        return when(type)
        {
            ESyncDataType.TRACKER->setSynchronizationFlagsImpl(OTTrackerDAO::class.java, idTimestampPair, {dao, stamp-> dao.synchronizedAt = stamp })
            ESyncDataType.TRIGGER->setSynchronizationFlagsImpl(OTTriggerDAO::class.java, idTimestampPair, {dao, stamp-> dao.synchronizedAt = stamp })
            ESyncDataType.ITEM->setSynchronizationFlagsImpl(OTItemDAO::class.java, idTimestampPair, {dao, stamp-> dao.synchronizedAt = stamp })
        }
    }

    override fun getDirtyRowsToSync(type: ESyncDataType): Single<List<Any>> {
        return when(type)
        {
            ESyncDataType.TRIGGER->getDirtyRowsToSyncImpl(OTTriggerDAO::class.java)
            ESyncDataType.TRACKER->getDirtyRowsToSyncImpl(OTTrackerDAO::class.java)
            ESyncDataType.ITEM->getDirtyRowsToSyncImpl(OTItemDAO::class.java)
        }
    }

    override fun applyServerRowsToSync(type: ESyncDataType, list: List<Any>): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private fun <T: RealmObject> getDirtyRowsToSyncImpl(tableClass: Class<T>): Single<List<Any>>{
        return Single.defer{
            val realm = makeNewRealmInstance()
            realm.where(tableClass).equalTo(FIELD_SYNCHRONIZED_AT, null as Long?).findAllAsync().asFlowable().map { it->it.toList() }.first(emptyList())
                    .doOnSuccess { realm.close() }
        }
    }

    fun applyServerItemsToSync(itemList: List<OTItemPOJO>): Single<Boolean> {
        return Single.defer {
            val realm = makeNewRealmInstance()
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
                                removeItemImpl(match, true, realm)
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
}