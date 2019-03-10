package kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.containers.AnyValueWithTimestamp
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTItemBuilderDAO : RealmObject() {

    companion object {
        const val HOLDER_TYPE_INPUT_FORM = 0
        const val HOLDER_TYPE_TRIGGER = 1
        const val HOLDER_TYPE_SERVICE = 2
    }

    @PrimaryKey
    var id: Long = 0
    var createdAt: Long = System.currentTimeMillis()
    var tracker: OTTrackerDAO? = null

    @Index
    var holderType: Int = 0

    var data = RealmList<OTItemBuilderFieldValueEntry>()

    fun setValue(attributeLocalId: String, value: AnyValueWithTimestamp?) {
        val match = data.find { it.attributeLocalId == attributeLocalId }
        if (match != null) {
            if (value == null) {
                if (match.isManaged)
                    match.deleteFromRealm()
                data.remove(match)
            } else {
                match.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                match.timestamp = value.timestamp ?: 0
            }
        } else {
            if (value != null) {
                val newEntryDao = OTItemBuilderFieldValueEntry()
                newEntryDao.attributeLocalId = attributeLocalId
                newEntryDao.serializedValue = value.value?.let { TypeStringSerializationHelper.serialize(it) }
                newEntryDao.timestamp = value.timestamp ?: 0
                data.add(newEntryDao)
            }
        }
    }
}

open class OTItemBuilderFieldValueEntry : RealmObject() {
    @PrimaryKey
    var id: Long = -1

    var attributeLocalId: String? = null
    var serializedValue: String? = null
    var timestamp: Long = System.currentTimeMillis()
}