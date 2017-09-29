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

/**
 * Created by younghokim on 2017. 9. 25..
 */
open class OTItemDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    @Index
    var trackerObjectId: String = ""

    var deviceId: String? = null

    @Index
    var timestamp: Long = 0

    var source: String? = null

    var fieldValueEntries = RealmList<OTItemAttributeEntryDAO>()

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
        val table = Hashtable<String, String>()
        for (entryDAO in fieldValueEntries) {
            table[entryDAO.attributeId] = entryDAO.serializedValue
        }
        return table
    }
}

open class OTItemAttributeEntryDAO : RealmObject() {
    var attributeId: String = ""
    var serializedValue: String? = null
}

object RealmItemHelper {

    const val TIMESTAMP_NULL = -1

    fun convertItemToDAO(item: OTItem): OTItemDAO {
        val dao = OTItemDAO()
        dao.trackerObjectId = item.trackerObjectId
        dao.objectId = item.objectId
        dao.deviceId = item.deviceId
        dao.source = item.source.name

        dao.timestamp = if (item.timestamp != -1L) {
            item.timestamp
        } else {
            System.currentTimeMillis()
        }

        for (entry in item.getEntryIterator()) {
            dao.fieldValueEntries.add(
                    OTItemAttributeEntryDAO().apply {
                        attributeId = entry.key
                        serializedValue = TypeStringSerializationHelper.serialize(entry.value)
                    }
            )
        }

        return dao
    }

    fun convertDAOToItem(dao: OTItemDAO): OTItem =//objectId notNull is guaranteed.
            OTItem(dao.objectId ?: dao.trackerObjectId + UUID.randomUUID().toString(), dao.trackerObjectId, dao.serializedValueTable(), dao.timestamp, dao.loggingSource, dao.deviceId)


    fun applyDaoToPojo(dao: OTItemDAO, pojo: OTItemPOJO) {
        pojo.objectId = dao.objectId ?: ""
        pojo.deviceId = dao.deviceId
        pojo.source = dao.source
        pojo.synchronizedAt = dao.synchronizedAt
        pojo.timestamp = dao.timestamp
        pojo.trackerObjectId = dao.trackerObjectId
        pojo.removed = dao.removed

        pojo.serializedValueTable = dao.serializedValueTable()
    }

    fun applyPojoToDao(pojo: OTItemPOJO, dao: OTItemDAO, realm: Realm) {
        dao.deviceId = pojo.deviceId
        dao.source = pojo.source
        dao.synchronizedAt = pojo.synchronizedAt
        dao.timestamp = pojo.timestamp
        dao.trackerObjectId = pojo.trackerObjectId
        dao.removed = pojo.removed

        dao.fieldValueEntries.clear()
        if (pojo.serializedValueTable != null) {
            dao.fieldValueEntries.addAll(
                    pojo.serializedValueTable!!.map { entry ->
                        realm.createObject(OTItemAttributeEntryDAO::class.java).apply {
                            this.attributeId = entry.key
                            this.serializedValue = entry.value
                        }
                    }.toTypedArray()
            )
        }
    }
}