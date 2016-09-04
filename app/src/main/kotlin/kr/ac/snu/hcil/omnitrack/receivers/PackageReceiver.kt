package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.core.OTShortcutManager

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action)
        {
            Intent.ACTION_INSTALL_PACKAGE->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }

            Intent.ACTION_PACKAGE_REPLACED->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }

            Intent.ACTION_PACKAGE_ADDED->{
                OTShortcutManager.refreshNotificationShortcutViews(context)
            }
        }
    }
}