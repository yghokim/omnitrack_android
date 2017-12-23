package kr.ac.snu.hcil.omnitrack.core.database.configured

import android.content.Intent
import com.google.gson.JsonObject
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import io.realm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.*
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimePoint
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.di.Configured
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncResultEntry
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 9. 25..
 */
@Configured
class BackendDbManager @Inject constructor(
        @Backend private val config: RealmConfiguration,
        private val shortcutPanelManager: Lazy<OTShortcutPanelManager>,
        private val serializationManager: DaoSerializationManager,
        private val triggerSystemManager: OTTriggerSystemManager,
        private val localCacheManager: OTLocalMediaCacheManager
) : ISynchronizationClientSideAPI {

    companion object {
        const val FIELD_OBJECT_ID = "objectId"
        const val FIELD_UPDATED_AT_LONG = "userUpdatedAt"
        const val FIELD_USER_CREATED_AT = "userCreatedAt"
        const val FIELD_SYNCHRONIZED_AT = "synchronizedAt"
        const val FIELD_REMOVED_BOOLEAN = "removed"
        const val FIELD_TIMESTAMP_LONG = "timestamp"

        const val FIELD_IS_IN_TRASHCAN = "isInTrashcan"
        const val FIELD_IS_HIDDEN = "isHidden"

        const val FIELD_LOCKED_PROPERTIES_SERIALIZED = "lockedProperties"


        const val FIELD_NAME = "name"
        const val FIELD_POSITION = "position"

        const val FIELD_USER_ID = "userId"
        const val FIELD_TRACKER_ID = "trackerId"

        const val SAVE_RESULT_NEW = 1
        const val SAVE_RESULT_EDIT = 2
        const val SAVE_RESULT_FAIL = 0
    }

    fun makeNewRealmInstance(): Realm = Realm.getInstance(config)

    val OnItemListUpdated = PublishSubject.create<String>() // trackerId

    fun getTrackerQueryWithId(objectId: String, realm: Realm): RealmQuery<OTTrackerDAO> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_OBJECT_ID, objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }

    fun getTriggerQueryWithId(objectId: String, realm: Realm): RealmQuery<OTTriggerDAO> {
        return realm.where(OTTriggerDAO::class.java).equalTo(FIELD_OBJECT_ID, objectId).equalTo(FIELD_REMOVED_BOOLEAN, false)
    }


    fun makeBookmarkedTrackersQuery(userId: String, realm: Realm): RealmQuery<OTTrackerDAO>
            = realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId).equalTo("isBookmarked", true)

    fun makeBookmarkedTrackersObservable(userId: String, realm: Realm): Flowable<RealmResults<OTTrackerDAO>> {
        return realm.where(OTTrackerDAO::class.java).equalTo(FIELD_REMOVED_BOOLEAN, false).equalTo(FIELD_USER_ID, userId).equalTo("isBookmarked", true).sort("position", Sort.ASCENDING).findAllAsync()
                .asFlowable()
    }

    fun makeShortcutPanelRefreshObservable(userId: String, realm: Realm): Flowable<RealmResults<OTTrackerDAO>> {
        return makeBookmarkedTrackersObservable(userId, realm).filter { it.isValid && it.isLoaded }
                .doAfterNext { list ->
                    shortcutPanelManager.get().refreshNotificationShortcutViews(list, OTApp.instance)
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

    fun getItemsQueriedWithTimeAttribute(trackerId: String?, scope: TimeSpan, timeAttributeLocalId: String, realm: Realm, queryInjection: ((RealmQuery<OTItemValueEntryDAO>) -> RealmQuery<OTItemValueEntryDAO>)? = null): List<OTItemDAO> {
        return realm.where(OTItemValueEntryDAO::class.java)
                .run {
                    if (trackerId != null) {
                        return@run this.equalTo("items.trackerId", trackerId)
                    } else return@run this
                }
                .run {
                    if (queryInjection != null) {
                        return@run queryInjection(this)
                    } else return@run this
                }
                .equalTo("items.removed", false)
                .isNotNull("value")
                .equalTo("key", timeAttributeLocalId)
                .beginGroup()
                .beginsWith("value", "${TypeStringSerializationHelper.TYPENAME_TIMEPOINT.length}${TypeStringSerializationHelper.TYPENAME_TIMEPOINT}")
                .or()
                .beginsWith("value", "${TypeStringSerializationHelper.TYPENAME_TIMESPAN.length}${TypeStringSerializationHelper.TYPENAME_TIMESPAN}")
                .endGroup()
                .findAll().filter {
            val time = TypeStringSerializationHelper.deserialize(it.value!!)
            return@filter if (time is TimePoint) {
                time.timestamp >= scope.from && time.timestamp < scope.to
            } else if (time is TimeSpan) {
                return@filter time.from < scope.to && scope.from < time.to
            } else false
        }.mapNotNull { it.items?.firstOrNull() }
    }

    fun getItemCountDuring(trackerId: String?, realm: Realm, offsetFromToday: Int = 0, overrideTimeColumnLocalId: String?): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, offsetFromToday) // offset from today

        val first = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val second = cal.timeInMillis

        if (overrideTimeColumnLocalId == null) {
            //use timestamp column
            return makeItemsQuery(trackerId, first, second, realm).count()
        } else {
            val baseQuery = makeItemsQuery(trackerId, null, null, realm)
            return baseQuery.equalTo("fieldValueEntries.key", overrideTimeColumnLocalId)
                    .findAll()
                    .filter {
                        val timeValue = it.getValueOf(overrideTimeColumnLocalId)
                        if (timeValue is TimePoint) {
                            return@filter timeValue.timestamp >= first && timeValue.timestamp < second
                        } else if (timeValue is TimeSpan) {
                            return@filter timeValue.from < second && first < timeValue.to
                        } else return@filter false
                    }.count().toLong()
        }
    }

    fun makeItemsQueryOfTheDay(trackerId: String?, realm: Realm, offsetFromToday: Int = 0): RealmQuery<OTItemDAO> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, offsetFromToday) // offset from today

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
                    dao.userUpdatedAt = System.currentTimeMillis()
                }
            }
        } else {
            realm.executeTransactionIfNotIn {
                dao.attributes.forEach { it.properties.deleteAllFromRealm() }
                dao.attributes.deleteAllFromRealm()
                dao.deleteFromRealm()
            }
        }
    }

    fun saveTrigger(dao: OTTriggerDAO, realm: Realm) {
        if (dao.isManaged) {
            triggerSystemManager.tryCheckInToSystem(dao)
        } else {
            if (realm.isInTransaction) {
                realm.copyToRealmOrUpdate(dao)
                val managedDao = realm.copyToRealmOrUpdate(dao)
                triggerSystemManager.tryCheckInToSystem(managedDao)
            } else {
                realm.executeTransactionAsync({ realm ->
                    if (dao.trackers.isNotEmpty()) {
                        val trackers = realm.where(OTTrackerDAO::class.java).`in`("objectId", dao.trackers.map { it.objectId }.toTypedArray()).findAll()
                        dao.trackers.clear()
                        dao.trackers.addAll(trackers)
                    }
                    val managedDao = realm.copyToRealmOrUpdate(dao)
                    triggerSystemManager.tryCheckInToSystem(managedDao)
                }, { err -> err.printStackTrace() })
            }
        }
    }

    fun removeTrigger(dao: OTTriggerDAO, permanently: Boolean, realm: Realm) {
        realm.executeTransactionIfNotIn { realm ->
            if (permanently) {
                dao.deleteFromRealm()
                triggerSystemManager.tryCheckOutFromSystem(dao)
            } else {
                dao.synchronizedAt = null
                dao.userUpdatedAt = System.currentTimeMillis()
                dao.removed = true
                if (!dao.isManaged) {
                    val managed = realm.copyToRealmOrUpdate(dao)
                    triggerSystemManager.tryCheckOutFromSystem(managed)
                } else triggerSystemManager.tryCheckOutFromSystem(dao)
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

                realm.executeTransactionIfNotIn {
                    item.fieldValueEntries.filter { localIdsToIgnore?.contains(it.key) != true }.forEach { entry ->
                        val value = entry.value?.let { TypeStringSerializationHelper.deserialize(it) }
                        if (value != null) {
                            if (value is OTServerFile && value.serverPath.isNotBlank()) {
                                if (localCacheManager.isServerPathTemporal(value.serverPath)) {
                                    val newServerPath = localCacheManager.replaceTemporalServerPath(value.serverPath, item.trackerId ?: "noTracker", item.objectId!!, entry.key)
                                    if (newServerPath != null) {
                                        value.serverPath = newServerPath
                                        entry.value = TypeStringSerializationHelper.serialize(value)
                                        localCacheManager.registerSynchronization()
                                    }
                                }
                            }
                        }
                    }
                    realm.copyToRealmOrUpdate(item)
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
                    itemDao.userUpdatedAt = System.currentTimeMillis()
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

    override fun getLatestSynchronizedServerTimeOf(type: ESyncDataType): Long
            = when (type) {
        ESyncDataType.ITEM -> getLatestSynchronizedServerTimeImpl(OTItemDAO::class.java)
        ESyncDataType.TRIGGER -> getLatestSynchronizedServerTimeImpl(OTTriggerDAO::class.java)
        ESyncDataType.TRACKER -> getLatestSynchronizedServerTimeImpl(OTTrackerDAO::class.java)
        else -> Long.MIN_VALUE
    }

    private fun getLatestSynchronizedServerTimeImpl(daoClass: Class<out RealmObject>): Long {
        val realm = makeNewRealmInstance()
        val max = realm.where(daoClass).max(FIELD_SYNCHRONIZED_AT)?.toLong() ?: 0
        realm.close()
        return max
    }

    private fun <T : RealmObject> setSynchronizationFlagsImpl(daoClass: Class<T>, idTimestampPair: List<SyncResultEntry>, setFlagFunc: (T, Long) -> Unit): Completable {
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
        return when (type) {
            ESyncDataType.TRACKER -> setSynchronizationFlagsImpl(OTTrackerDAO::class.java, idTimestampPair, { dao, stamp -> dao.synchronizedAt = stamp })
            ESyncDataType.TRIGGER -> setSynchronizationFlagsImpl(OTTriggerDAO::class.java, idTimestampPair, { dao, stamp -> dao.synchronizedAt = stamp })
            ESyncDataType.ITEM -> setSynchronizationFlagsImpl(OTItemDAO::class.java, idTimestampPair, { dao, stamp -> dao.synchronizedAt = stamp })
        }
    }

    override fun getDirtyRowsToSync(type: ESyncDataType): Single<List<String>> {
        return when (type) {
            ESyncDataType.TRIGGER -> getDirtyRowsToSyncImpl(OTTriggerDAO::class.java, { trigger -> serializationManager.serializeTrigger(trigger, true) })
            ESyncDataType.TRACKER -> getDirtyRowsToSyncImpl(OTTrackerDAO::class.java, { tracker -> serializationManager.serializeTracker(tracker, true) })
            ESyncDataType.ITEM -> getDirtyRowsToSyncImpl(OTItemDAO::class.java, { item -> serializationManager.serializeItem(item, true) })
        }
    }

    override fun applyServerRowsToSync(type: ESyncDataType, jsonList: List<JsonObject>): Completable {
        return when (type) {
            ESyncDataType.TRACKER -> applyServerRowsToSyncImpl(OTTrackerDAO::class.java, jsonList, { tracker -> tracker.synchronizedAt },
                    { tracker, realm -> realm.copyToRealmOrUpdate(tracker) },
                    { tracker, realm -> removeTracker(tracker, true, realm) },
                    null,
                    { dao, serverPojo -> dao.userUpdatedAt < serverPojo.get(FIELD_UPDATED_AT_LONG).asLong }, serializationManager.serverTrackerTypeAdapter as Lazy<JsonObjectApplier<OTTrackerDAO>>)
            ESyncDataType.TRIGGER -> applyServerRowsToSyncImpl(OTTriggerDAO::class.java, jsonList, { trigger -> trigger.synchronizedAt },
                    { trigger, realm -> saveTrigger(trigger, realm) },
                    { trigger, realm -> removeTrigger(trigger, true, realm) },

                    { trigger, realm ->
                        if (trigger.removed) {
                            triggerSystemManager.tryCheckOutFromSystem(trigger)
                        } else {
                            triggerSystemManager.tryCheckInToSystem(trigger)
                        }
                    },
                    { dao, serverPojo -> dao.userUpdatedAt < serverPojo.get(FIELD_UPDATED_AT_LONG).asLong }, serializationManager.serverTriggerTypeAdapter as Lazy<JsonObjectApplier<OTTriggerDAO>>)
            ESyncDataType.ITEM -> applyServerRowsToSyncImpl(OTItemDAO::class.java, jsonList, { item -> item.synchronizedAt },
                    { item, realm -> realm.copyToRealmOrUpdate(item) },
                    { item, realm -> removeItemImpl(item, true, realm) }, null, { dao, serverPojo -> dao.timestamp < serverPojo.get(FIELD_TIMESTAMP_LONG).asLong }, serializationManager.serverItemTypeAdapter as Lazy<JsonObjectApplier<OTItemDAO>>)
        }
    }

    private fun <T : RealmObject> applyServerRowsToSyncImpl(
            tableClass: Class<T>,
            serverRowJsonList: List<JsonObject>,
            synchronizedAtFunc: (T) -> Long?,
            saveRowInDbFunc: (T, Realm) -> Unit,
            removeRowInDbFunc: (T, Realm) -> Unit,
            afterRowUpdatedFunc: ((T, Realm) -> Unit)?,
            isServerWinForResolutionFunc: (T, JsonObject) -> Boolean, applier: Lazy<JsonObjectApplier<T>>): Completable {
        return Completable.defer {
            val realm = makeNewRealmInstance()
            try {
                for (serverPojo in serverRowJsonList.filter { it.has(FIELD_OBJECT_ID) }) {
                    val match = realm.where(tableClass).equalTo(FIELD_OBJECT_ID, serverPojo.get(FIELD_OBJECT_ID).asString).findFirst()
                    if (match == null) {
                        if (serverPojo.get(FIELD_REMOVED_BOOLEAN)?.asBoolean != true) {
                            //insert
                            println("synchronization: server row not matched and is not a removed row. append new in local db.")

                            try {
                                realm.executeTransaction { realm ->
                                    saveRowInDbFunc.invoke(applier.get().decodeToDao(serverPojo), realm)
                                }
                            } catch (ex: Exception) {
                                println("synchronization failed. skip object: $serverPojo")
                                ex.printStackTrace()
                            }
                        }
                    } else if (synchronizedAtFunc(match) == null) {
                        //found matched row, but it is dirty. Conflict!
                        //late timestamp win policy
                        println("conflict")
                        if (isServerWinForResolutionFunc(match, serverPojo)) {
                            //server win
                            println("server win")
                            realm.executeTransaction {
                                removeRowInDbFunc(match, realm)
                                //removeItemImpl(match, true, realm)
                            }
                        } else {
                            //client win
                            println("client win")
                        }
                    } else {
                        //update
                        realm.executeTransaction {
                            if (serverPojo.get(FIELD_REMOVED_BOOLEAN)?.asBoolean == true) {
                                removeRowInDbFunc(match, realm)
                                //removeItemImpl(match, true, realm)
                            } else {
                                applier.get().applyToManagedDao(serverPojo, match)
                            }
                        }
                    }
                }

                realm.close()
                return@defer Completable.complete()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@defer Completable.error(ex)
            } finally {
                realm.close()
            }
        }
    }


    private fun <T : RealmObject> getDirtyRowsToSyncImpl(tableClass: Class<T>, serialize: (T) -> String): Single<List<String>> {
        return Single.defer {
            val realm = makeNewRealmInstance()
            val list = realm.where(tableClass).equalTo(FIELD_SYNCHRONIZED_AT, null as Long?).findAll().map { it -> serialize(it) }.toList()
            realm.close()
            return@defer Single.just(list)
        }
    }
}