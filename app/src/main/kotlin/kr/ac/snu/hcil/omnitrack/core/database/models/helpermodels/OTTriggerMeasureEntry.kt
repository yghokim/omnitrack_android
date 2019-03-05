package kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO

open class OTTriggerMeasureEntry : RealmObject() {

    @PrimaryKey
    var id: Long = 0

    @Required
    @Index
    var factoryCode: String? = null

    @Required
    var serializedMeasure: String? = null

    @Required
    var serializedTimeQuery: String? = null

    var measureHistory = RealmList<OTTriggerMeasureHistoryEntry>()

    var triggers = RealmList<OTTriggerDAO>()

}

open class OTTriggerMeasureHistoryEntry : RealmObject() {
    @PrimaryKey
    var id: Long = 0
    var measuredValue: Double? = null

    @Index
    var timestamp: Long = System.currentTimeMillis()
}