package kr.ac.snu.hcil.omnitrack.services

import com.google.android.gms.gcm.*
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.triggers.OTDataTriggerManager

class DataTriggerCheckService : GcmTaskService() {

    companion object {
        const val TAG_DATA_TRIGGER_CHECK = "dataTriggerCompare"

        fun makeTask(periodSecond: Long): PeriodicTask {
            return PeriodicTask.Builder()
                    .setService(DataTriggerCheckService::class.java)
                    .setPeriod(periodSecond)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setTag(TAG_DATA_TRIGGER_CHECK)
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .build()
        }
    }

    override fun onRunTask(params: TaskParams): Int {

        when (params.tag) {
            TAG_DATA_TRIGGER_CHECK -> {
                OTDataTriggerManager.checkMeasures(OTApp.instance)

                return GcmNetworkManager.RESULT_SUCCESS
            }
            else -> return GcmNetworkManager.RESULT_FAILURE
        }
    }

}
