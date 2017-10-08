package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.database.abstraction.pojos.OTItemPOJO
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by younghokim on 2017. 9. 25..
 */
open class OTItemDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    @Index
    var trackerId: String = ""

    var deviceId: String? = null

    @Index
    var timestamp: Long = 0

    var source: String? = null

    var fieldValueEntries = RealmList<OTStringStringEntryDAO>()

    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.

    var updatedAt: Long = System.currentTimeMillis()

    var removed: Boolean = false

    var loggingSource: OTItem.LoggingSource
        get() = if (source != null) {
            OTItem.LoggingSource.valueOf(source!!)
        } else OTItem.LoggingSource.Unspecified
        set(value) {
            source = value.name
        }

    fun serializedValueTable(): Map<String, String> {
        return RealmDatabaseManager.convertRealmEntryListToDictionary(fieldValueEntries)
    }
}

object RealmItemHelper {

    fun applyItemToDAO(item: OTItem, dao: OTItemDAO, realm: Realm) {
        dao.trackerId = item.trackerId
        dao.deviceId = item.deviceId
        dao.source = item.source.name

        dao.timestamp = if (item.timestamp != -1L) {
            item.timestamp
        } else {
            System.currentTimeMillis()
        }

        val serializedTable = HashMap<String, String>()
        for (entry in item.getEntryIterator()) {
            serializedTable[entry.key] = TypeStringSerializationHelper.serialize(entry.value)
        }
        RealmDatabaseManager.convertDictionaryToRealmList(realm, serializedTable, dao.fieldValueEntries, null)
    }

    fun convertDAOToItem(dao: OTItemDAO): OTItem =//objectId notNull is guaranteed.
            OTItem(dao.objectId ?: dao.trackerId + UUID.randomUUID().toString(), dao.trackerId, dao.serializedValueTable(), dao.timestamp, dao.loggingSource, dao.deviceId)


    fun applyDaoToPojo(dao: OTItemDAO, pojo: OTItemPOJO) {
        pojo.objectId = dao.objectId ?: ""
        pojo.deviceId = dao.deviceId
        pojo.source = dao.source
        pojo.synchronizedAt = dao.synchronizedAt
        pojo.timestamp = dao.timestamp
        pojo.trackerObjectId = dao.trackerId
        pojo.removed = dao.removed

        pojo.serializedValueTable = dao.serializedValueTable()
    }

    fun applyPojoToDao(pojo: OTItemPOJO, dao: OTItemDAO, realm: Realm) {
        dao.deviceId = pojo.deviceId
        dao.source = pojo.source
        dao.synchronizedAt = pojo.synchronizedAt
        dao.timestamp = pojo.timestamp
        dao.trackerId = pojo.trackerObjectId
        dao.removed = pojo.removed

        if (pojo.serializedValueTable != null) {
            RealmDatabaseManager.convertDictionaryToRealmList(
                    realm, pojo.serializedValueTable!!,
                    dao.fieldValueEntries, null)
        } else {
            dao.fieldValueEntries.forEach {
                it.deleteFromRealm()
            }
            dao.fieldValueEntries.clear()
        }
    }
}