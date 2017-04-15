package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import rx.schedulers.Schedulers

/**
 * Created by Young-Ho on 9/4/2016.
 */
class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("OMNITRACK: reboot receiver called")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                println("OMNITRACK: Android system rebooted")
                OTApplication.app.currentUserObservable.observeOn(Schedulers.immediate()).subscribe {
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                }

                OTVersionCheckService.setupServiceAlarm(context)
            }
        }
    }
}