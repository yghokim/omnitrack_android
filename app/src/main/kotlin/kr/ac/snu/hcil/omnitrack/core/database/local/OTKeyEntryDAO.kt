package kr.ac.snu.hcil.omnitrack.core.database.local

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by Young-Ho on 10/8/2017.
 */

open class OTStringStringEntryDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var key: String = ""
    var value: String? = null

    override fun toString(): String {
        return "{StringStringEntry | id : $id, key : $key, value : $value}"
    }
}

open class OTIntegerStringEntryDAO : RealmObject() {
    @PrimaryKey
    var id: String = ""
    var key: Int = -1
    var value: String? = null
}