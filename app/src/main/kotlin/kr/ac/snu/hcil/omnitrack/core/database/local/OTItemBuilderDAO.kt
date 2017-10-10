package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

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
}

open class OTItemBuilderFieldValueEntry : RealmObject() {
    @PrimaryKey
    var id: Long = 0

    var attributeLocalId: String? = null
    var serializedValue: String? = null
    var timestamp: Long = System.currentTimeMillis()
}