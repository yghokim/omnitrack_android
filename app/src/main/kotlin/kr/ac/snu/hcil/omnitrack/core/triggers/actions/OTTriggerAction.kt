package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import com.github.salomonbrys.kotson.jsonArray
import com.google.gson.JsonObject
import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 4. 17..
 */
abstract class OTTriggerAction {

    abstract fun performAction(trigger: OTTriggerDAO, triggerTime: Long, metadata: JsonObject, configuredContext: ConfiguredContext): Completable

    open fun writeEventLogContent(trigger: OTTriggerDAO, table: JsonObject) {
        table.add("trackers", jsonArray(*trigger.liveTrackerIds))
    }

    abstract fun getSerializedString(): String

    abstract fun clone(): OTTriggerAction
}