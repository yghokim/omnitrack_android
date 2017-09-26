package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.OTItem
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

    var synchronized: Boolean = false

    var updatedAt: Long = System.currentTimeMillis()


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
}