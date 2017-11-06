package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
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
    var trackerId: String? = null

    var deviceId: String? = null

    @Index
    var timestamp: Long = System.currentTimeMillis()

    var source: String? = null

    var fieldValueEntries = RealmList<OTItemValueEntryDAO>()

    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.

    var userUpdatedAt: Long = System.currentTimeMillis()

    var removed: Boolean = false

    var loggingSource: ItemLoggingSource
        get() = if (source != null) {
            ItemLoggingSource.valueOf(source!!)
        } else ItemLoggingSource.Unspecified
        set(value) {
            source = value.name
        }

    fun serializedValueTable(): Map<String, String> {
        val table = Hashtable<String, String>()
        for (entryDAO in fieldValueEntries) {
            table[entryDAO.key] = entryDAO.value
        }
        return table
    }

    fun setValueOf(attributeLocalId: String, serializedValue: String?): Boolean {

        val match = fieldValueEntries.find { it.key == attributeLocalId }
        return if (match != null) {
            if (match.value != serializedValue) {
                match.value = serializedValue
                true
            } else false
        } else {
            fieldValueEntries.add(
                    OTItemValueEntryDAO().apply {
                        id = UUID.randomUUID().toString()
                        key = attributeLocalId
                        value = serializedValue
                    }
            )
        }
    }

    fun getValueOf(attributeLocalId: String): Any? {
        return fieldValueEntries.find { it.key == attributeLocalId }?.value?.let { TypeStringSerializationHelper.deserialize(it) }
    }
}

object RealmItemHelper {

    fun applyDaoToPojo(dao: OTItemDAO, pojo: OTItemPOJO) {
        pojo.objectId = dao.objectId ?: ""
        pojo.deviceId = dao.deviceId
        pojo.source = dao.source
        pojo.synchronizedAt = dao.synchronizedAt
        pojo.timestamp = dao.timestamp
        pojo.trackerObjectId = dao.trackerId!!
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
            pojo.serializedValueTable!!.entries.forEach { (key, serializedValue) ->
                dao.setValueOf(key, serializedValue)
            }
        } else {
            dao.fieldValueEntries.forEach {
                it.deleteFromRealm()
            }
            dao.fieldValueEntries.clear()
        }
    }
}

open class OTItemValueEntryDAO : RealmObject() {

    @PrimaryKey
    var id: String = ""

    @Index
    var key: String = ""

    var value: String? = null

    override fun toString(): String {
        return "{Item Value Entry | id : $id, key : $key, value : $value}"
    }
}