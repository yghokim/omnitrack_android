package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

/**
 * Created by younghokim on 2017. 4. 17..
 */
abstract class OTTriggerAction {

    open lateinit var trigger: OTTriggerDAO

    abstract fun performAction(triggerTime: Long, context: Context): Single<OTTriggerDAO>

    abstract fun getSerializedString(): String?
}