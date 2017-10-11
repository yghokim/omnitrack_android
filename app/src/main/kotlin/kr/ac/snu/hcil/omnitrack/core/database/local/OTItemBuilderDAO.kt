package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTPendingItemBuilderDAO : RealmObject() {

    companion object {
        const val HOLDER_TYPE_INPUT_FORM = 0
        const val HOLDER_TYPE_TRIGGER = 1
    }

    @PrimaryKey
    var id: Long = 0
    var createdAt: Long = System.currentTimeMillis()
    var tracker: OTTrackerDAO? = null

    @Index
    var holderType: Int = 0

    var data = RealmList<OTItemBuilderFieldValueEntry>()

    fun setValue(attributeLocalId: String, value: OTItemBuilderWrapperBase.ValueWithTimestamp?, realm: Realm) {
        val match = data.find { it.attributeLocalId == attributeLocalId }
        if (match != null) {
            if (value == null) {
                match.deleteFromRealm()
                data.remove(match)
            } else {
                match.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                match.timestamp = value.timestamp
            }
        } else {
            if (value != null) {
                val highestId = realm.where(OTItemBuilderFieldValueEntry::class.java).max("id")?.toLong() ?: 0L
                val newEntryDao = OTItemBuilderFieldValueEntry()
                newEntryDao.id = highestId + 1
                newEntryDao.attributeLocalId = attributeLocalId
                newEntryDao.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                newEntryDao.timestamp = value.timestamp
                data.add(newEntryDao)
            }
        }
    }
}

open class OTItemBuilderFieldValueEntry : RealmObject() {
    @PrimaryKey
    var id: Long = 0

    var attributeLocalId: String? = null
    var serializedValue: String? = null
    var timestamp: Long = System.currentTimeMillis()
}