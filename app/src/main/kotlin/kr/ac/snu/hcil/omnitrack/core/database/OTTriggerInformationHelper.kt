package kr.ac.snu.hcil.omnitrack.core.database

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO

/**
 * Created by younghokim on 2017. 10. 21..
 */
object OTTriggerInformationHelper {
    @StringRes
    fun getConfigDescriptionResId(trigger: OTTriggerDAO): Int? {

        return null
    }

    fun getConfigSummaryText(trigger: OTTriggerDAO): CharSequence? {
        return null
    }

    @StringRes
    fun getConfigNameRestId(trigger: OTTriggerDAO): Int? {
        return null
    }

    @DrawableRes
    fun getConfigIconResId(trigger: OTTriggerDAO): Int? {
        return null
    }

    @StringRes
    fun getActionName(actionType: Byte): Int? {
        return when (actionType) {
            OTTriggerDAO.ACTION_TYPE_LOG -> R.string.msg_text_trigger
            OTTriggerDAO.ACTION_TYPE_REMIND -> R.string.msg_text_reminder
            else -> null
        }
    }
}