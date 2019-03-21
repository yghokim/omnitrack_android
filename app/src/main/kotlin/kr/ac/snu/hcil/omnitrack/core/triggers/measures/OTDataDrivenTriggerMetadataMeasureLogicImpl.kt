package kr.ac.snu.hcil.omnitrack.core.triggers.measures

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO

class OTDataDrivenTriggerMetadataMeasureLogicImpl(context: Context) : OTItemMetadataMeasureFactoryLogicImpl(context) {
    override fun checkAvailability(tracker: OTTrackerDAO, invalidMessages: MutableList<CharSequence>?): Boolean {
        return if (tracker.liveTriggersQuery?.equalTo("conditionType", OTTriggerDAO.CONDITION_TYPE_DATA)?.count() ?: 0 > 0) {
            true
        } else {
            invalidMessages?.add(context.getString(R.string.msg_trigger_data_measure_error_no_data_driven))
            false
        }
    }

}