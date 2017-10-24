package kr.ac.snu.hcil.omnitrack.ui.pages.trigger

import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.local.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.ui.activities.MultiButtonActionBarActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.trigger.viewmodels.TriggerInterfaceOptions

/**
 * Created by younghokim on 2017-10-24.
 */
class TriggerDetailActivity : MultiButtonActionBarActivity(R.layout.activity_trigger_detail) {
    companion object {
        const val MODE_NEW = "MODE_NEW_TRIGGER"
        const val MODE_EDIT = "MODE_EDIT_TRIGGER"
        const val INTENT_EXTRA_INTERFACE_OPTIONS = "trigger_interface_options"
        const val INTENT_EXTRA_TRIGGER_DAO = "trigger_dao"

        fun makeNewTriggerIntent(context: Context, baseDao: OTTriggerDAO, options: TriggerInterfaceOptions): Intent {
            val serialized = OTTriggerDAO.parser.toJson(baseDao, OTTriggerDAO::class.java)
            println(serialized)
            return Intent(context, TriggerDetailActivity::class.java)
                    .setAction(MODE_NEW)
                    .putExtra(INTENT_EXTRA_TRIGGER_DAO, serialized)
                    .putExtra(INTENT_EXTRA_INTERFACE_OPTIONS, options)
        }

        fun makeEditTriggerIntent(context: Context, triggerId: String, options: TriggerInterfaceOptions): Intent {
            return Intent(context, TriggerDetailActivity::class.java)
                    .setAction(MODE_NEW)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_INTERFACE_OPTIONS, options)
        }
    }


    override fun onToolbarLeftButtonClicked() {
    }

    override fun onToolbarRightButtonClicked() {
    }

}