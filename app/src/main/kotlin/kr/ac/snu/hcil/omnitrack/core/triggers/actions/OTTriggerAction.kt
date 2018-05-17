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

    open lateinit var trigger: OTTriggerDAO

    abstract fun performAction(triggerTime: Long, configuredContext: ConfiguredContext): Completable

    open fun writeEventLogContent(table: JsonObject) {
        table.add("trackers", jsonArray(*trigger.liveTrackersQuery.findAll().map { it.objectId!! }.toTypedArray()))
    }

    abstract fun getSerializedString(): String
}