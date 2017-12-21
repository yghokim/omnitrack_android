package kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels

import kr.ac.snu.hcil.omnitrack.core.database.local.models.OTTriggerDAO
import java.io.Serializable

/**
 * Created by younghokim on 2017. 10. 21..
 */
data class TriggerInterfaceOptions(
        val showAttachedTrackers: Boolean = true,
        val defaultAttachedTrackers: Array<String>? = null,
        val supportedConditionTypes: Array<Byte>? = null,
        val defaultActionType: Byte = OTTriggerDAO.ACTION_TYPE_LOG
) : Serializable