package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action)
        {
            Intent.ACTION_INSTALL_PACKAGE->{
                OTShortcutPanelManager.refreshNotificationShortcutViews(context)
            }

            Intent.ACTION_PACKAGE_REPLACED->{
                OTShortcutPanelManager.refreshNotificationShortcutViews(context)
            }

            Intent.ACTION_PACKAGE_ADDED->{
                OTShortcutPanelManager.refreshNotificationShortcutViews(context)
            }
        }
    }
}