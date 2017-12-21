package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by younghokim on 2017. 11. 12..
 */
abstract class TriggerFireBroadcastReceiver : BroadcastReceiver() {
    companion object {
        fun makeIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(OTApp.BROADCAST_ACTION_TRIGGER_FIRED)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == OTApp.BROADCAST_ACTION_TRIGGER_FIRED) {
            onTriggerFired(intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER), intent.getLongExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis()))
        }
    }

    protected abstract fun onTriggerFired(triggerId: String, triggerTime: Long)
}