package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        println("package broadcast receiver")
        when (intent.action) {
            Intent.ACTION_INSTALL_PACKAGE -> {
                /*
                OTApplication.app.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }, {})*/
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                println("app package replaced")
                /*
                OTApplication.app.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                    if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
                        DatabaseManager.getDeviceInfoChild()?.child("appVersion")?.setValue(BuildConfig.VERSION_NAME)
                    }
                }, {})

*/
                OTVersionCheckService.setupServiceAlarm(context)
            }

            Intent.ACTION_PACKAGE_ADDED -> {
                /*
                OTApplication.app.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }, {})*/
            }
        }
    }
}