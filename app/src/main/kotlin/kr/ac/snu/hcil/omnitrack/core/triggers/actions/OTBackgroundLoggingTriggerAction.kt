package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService

/**
 * Created by younghokim on 2017. 4. 17..
 */
class OTBackgroundLoggingTriggerAction(override var trigger: OTTriggerDAO) : OTTriggerAction() {
    override fun getSerializedString(): String? {
        return null
    }

    override fun performAction(triggerTime: Long, context: Context): Completable {
        return Completable.defer {
            if (trigger.trackers.isNotEmpty()) {
                context.startService(
                        OTItemLoggingService.makeLoggingIntent(context, ItemLoggingSource.Trigger, *(trigger.trackers.map { it.objectId!! }.toTypedArray()))
                )
            }
            Completable.complete()
        }
    }


}