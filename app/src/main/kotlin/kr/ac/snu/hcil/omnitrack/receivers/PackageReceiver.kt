package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.widgets.OTShortcutPanelWidgetUpdateService
import rx.schedulers.Schedulers

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("package broadcast receiver")
        OTApplication.app.currentUserObservable.observeOn(Schedulers.immediate()).subscribe {
            user ->
            when (intent.action) {
                Intent.ACTION_INSTALL_PACKAGE -> {
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }

                Intent.ACTION_PACKAGE_REPLACED -> {
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }

                Intent.ACTION_PACKAGE_ADDED -> {
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }
            }
        }
    }
}