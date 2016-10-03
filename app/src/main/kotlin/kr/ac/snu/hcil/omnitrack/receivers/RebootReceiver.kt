package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutManager

/**
 * Created by Young-Ho on 9/4/2016.
 */
class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action)
        {
            Intent.ACTION_BOOT_COMPLETED->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }
        }
    }
}