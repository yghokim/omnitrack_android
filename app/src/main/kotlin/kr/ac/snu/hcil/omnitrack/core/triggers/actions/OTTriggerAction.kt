package kr.ac.snu.hcil.omnitrack.core.triggers.actions

import android.content.Context
import io.reactivex.Completable
import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO

/**
 * Created by younghokim on 2017. 4. 17..
 */
abstract class OTTriggerAction {

    open lateinit var trigger: OTTriggerDAO

    abstract fun performAction(triggerTime: Long, context: Context): Completable

    abstract fun getSerializedString(): String?
}