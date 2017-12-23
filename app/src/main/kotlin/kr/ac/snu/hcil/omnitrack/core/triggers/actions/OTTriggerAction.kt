package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 4. 17..
 */
abstract class OTTriggerAction {

    open lateinit var trigger: OTTriggerDAO

    abstract fun performAction(triggerTime: Long, configuredContext: ConfiguredContext): Completable

    abstract fun getSerializedString(): String
}