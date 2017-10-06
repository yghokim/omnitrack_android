package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.exceptions.RealmException
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import java.util.*

/**
 * Created by younghokim on 2017. 10. 1..
 */
open class OTTrackerDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    @Index
    var userId: String? = null

    var name: String = ""

    var position: Int = 0
    var color: Int = 0
    var attributeLocalKeySeed: Int = 0
    var isBookmarked: Boolean = false

    var attributes = RealmList<OTAttributeDAO>()
    var removedAttributes = RealmList<OTAttributeDAO>()

    var creationFlags = RealmList<OTKeyStringValueEntryDAO>()
    var isEditable: Boolean = true

    var userCreatedAt: Long = System.currentTimeMillis()
    var synchronizedAt: Long? = null // store server time of when synchronized perfectly.
    var updatedAt: Long = System.currentTimeMillis()
    var removed: Boolean = false

    val creationFlagsMap: Map<String, String> get() = RealmDatabaseManager.convertRealmEntryListToDictionary(creationFlags)


    companion object {
        fun convertDaoToTracker(dao: OTTrackerDAO): OTTracker {
            return OTTracker(dao.objectId, dao.name, dao.color, dao.isBookmarked,
                    dao.isEditable, dao.attributeLocalKeySeed,
                    dao.attributes.map { attrDao ->
                        OTAttribute.Companion.createAttribute(
                                attrDao.objectId,
                                attrDao.localId,
                                null,
                                attrDao.name,
                                attrDao.isRequired,
                                attrDao.type,
                                attrDao.serializedProperties,
                                attrDao.serializedConnection
                        )
                    },
                    dao.creationFlagsMap,
                    dao.position)
        }
    }
}


open class OTAttributeDAO : RealmObject() {

    @PrimaryKey
    var objectId: String? = null

    var localId: Int = -1

    @Index
    var trackerId: String? = null

    var name: String = ""

    var position: Int = 0
    var serializedConnection: String? = null
    var type: Int = -1
    var isRequired: Boolean = false
    var properties = RealmList<OTKeyStringValueEntryDAO>()

    var userCreatedAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    val serializedProperties: Map<String, String>?
        get() = if (properties.isNotEmpty()) {
            RealmDatabaseManager.convertRealmEntryListToDictionary(properties)
        } else null

    companion object {
        fun convertAttributeToDAO(attribute: OTAttribute<*>, position: Int, realm: Realm, dao: OTAttributeDAO?): OTAttributeDAO {
            if (!realm.isInTransaction) {
                throw RealmException("This operation should be called in transaction.")
            }

            val baseDao = dao ?: realm.createObject(OTAttributeDAO::class.java, attribute.objectId)
            baseDao.localId = attribute.localKey
            baseDao.name = attribute.name
            baseDao.position = position
            baseDao.isRequired = attribute.isRequired
            baseDao.serializedConnection = attribute.valueConnection?.getSerializedString()

            val properties = HashMap<String, String>()
            attribute.writePropertiesToDatabase(properties)

            RealmDatabaseManager.convertDictionaryToRealmList(realm,
                    properties,
                    baseDao.properties,
                    null)

            return baseDao
        }
    }
}