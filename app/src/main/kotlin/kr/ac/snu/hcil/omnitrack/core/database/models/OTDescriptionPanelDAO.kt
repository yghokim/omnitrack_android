package kr.ac.snu.hcil.omnitrack.core.database.models

import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey

open class OTDescriptionPanelDAO : RealmObject() {

    @Suppress("PropertyName")
    @PrimaryKey
    var _id: String = ""

    @Index
    var trackerId: String = ""

    var content: String = ""

    var serializedCreationFlags: String = "{}"

}