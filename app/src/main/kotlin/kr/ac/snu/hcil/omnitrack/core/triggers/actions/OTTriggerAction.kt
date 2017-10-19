package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger

/**
 * Created by younghokim on 2017. 4. 17..
 */
abstract class OTTriggerAction(val trigger: OTTrigger) {

    companion object {
        fun extractTriggerActionInstance(trigger: OTTrigger): OTTriggerAction {
            return when (trigger.action) {
                OTTrigger.ACTION_NOTIFICATION -> OTNotificationTriggerAction(trigger)
                OTTrigger.ACTION_BACKGROUND_LOGGING -> OTBackgroundLoggingTriggerAction(trigger)
                else -> throw IllegalArgumentException("Unsupported trigger action type")
            }
        }
    }

    abstract fun performAction(triggerTime: Long, context: Context): Single<OTTrigger>

}