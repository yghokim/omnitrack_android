package kr.ac.snu.hcil.omnitrack.core.database.local

import android.content.Intent
import android.net.Uri
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
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
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

    val OnItemListUpdated = PublishSubject.create<String>() // trackerId

    private fun getItemQueryOfTracker(tracker: OTTracker, realm: Realm): RealmQuery<OTItemDAO> {
        return realm.where(OTItemDAO::class.java).equalTo(FIELD_TRACKER_ID, tracker.objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getTrackerQueryWithId(objectId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_OBJECT_ID, objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getAttributeListQuery(trackerId: String, realm: Realm): RealmQuery<OTAttributeDAO> {
        return realm.where(OTAttributeDAO::class.java).equalTo(FIELD_TRACKER_ID, trackerId)
    }

    fun getUnManagedTrackerDao(trackerId: String?, realm: Realm?): OTTrackerDAO? {
        if (trackerId == null) {
            return null
        }

        val realmInstance = if (realm != null) {
            realm
        } else getRealmInstance()

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

    fun getItemBuilderQuery(trackerId: String, holderType: Int, realm: Realm): RealmQuery<OTItemBuilderDAO> {
        return realm.where(OTItemBuilderDAO::class.java).equalTo("tracker.objectId", trackerId).equalTo("holderType", holderType)
    }

    fun makeTriggersOfUserQuery(userId: String, realm: Realm): RealmQuery<OTTriggerDAO> {
        return realm.where(OTTriggerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId)
    }

    fun makeTrackersOfUserQuery(userId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId)
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
                }
            }
        }
    }

    fun addNewTrigger(dao: OTTriggerDAO, realm: Realm) {
        if (realm.isInTransaction) {
            realm.copyToRealmOrUpdate(dao)
        } else {
            realm.executeTransaction { realm ->
                realm.copyToRealm(dao)
            }
        }
    }

    fun removeTrigger(dao: OTTriggerDAO, realm: Realm) {
        fun process(realm: Realm) {
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

        item.fieldValueEntries.filter { !(localIdsToIgnore?.contains(it.key) ?: false) }.forEach { entry ->
            val value = entry.value?.let { TypeStringSerializationHelper.deserialize(it) }
            if (value != null) {
                if (value is SynchronizedUri && value.localUri != Uri.EMPTY) {
                    println("upload Synchronized Uri file to server...")
                    value.setSynchronized(OTApp.instance.binaryUploadServiceController.makeFilePath(itemId, item.trackerId!!, OTAuthManager.userId!!, value.localUri.lastPathSegment))
                    entry.value = TypeStringSerializationHelper.serialize(value)
                    OTApp.instance.startService(
                            OTApp.instance.binaryUploadServiceController.makeUploadServiceIntent(value, itemId, item.trackerId!!, OTAuthManager.userId!!)
                    )
                }
            }
        }
    }

    fun removeItem(itemDao: OTItemDAO, realm: Realm) {
        val trackerId = itemDao.trackerId
        if (removeItemImpl(itemDao, realm)) {

            val intent = Intent(OTApp.BROADCAST_ACTION_ITEM_REMOVED)

            intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)

            OTApp.instance.sendBroadcast(intent)

            OnItemListUpdated.onNext(trackerId!!)
        } else {
            println("item remove failed.")
        }
    }

    private fun removeItemImpl(itemDao: OTItemDAO, realm: Realm): Boolean {
        try {
            fun command() {
                itemDao.fieldValueEntries.forEach {
                    it.deleteFromRealm()
                }

                itemDao.removed = true
                itemDao.synchronizedAt = null
                itemDao.updatedAt = System.currentTimeMillis()

            }

            if (!realm.isInTransaction) {
                realm.executeTransaction { command() }
            } else {
                command()
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
}