package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver : BroadcastReceiver() {

    @Inject lateinit var versionCheckServiceController: OTVersionCheckService.Controller

    override fun onReceive(context: Context, intent: Intent) {

        (context.applicationContext as OTApp).jobDispatcherComponent.inject(this)

        println("package broadcast receiver - ${intent.action}")

        val authManager = (context.applicationContext as OTApp).currentConfiguredContext.configuredAppComponent.getAuthManager()
        if (authManager.isUserSignedIn()) {

            val reminderCommands = OTReminderCommands((context.applicationContext as OTApp).currentConfiguredContext, context)
            val realm = (context.applicationContext as OTApp).currentConfiguredContext.configuredAppComponent.backendRealmFactory().get()
            reminderCommands.restoreReminderNotifications(realm).blockingAwait()
            realm.close()

            val triggerSystemManager = (context.applicationContext as OTApp).currentConfiguredContext.triggerSystemComponent.getTriggerSystemManager().get()
            triggerSystemManager.checkInAllToSystem(authManager.userId!!)
        }


        when (intent.action) {
            Intent.ACTION_INSTALL_PACKAGE -> {
                /*
                OTApp.instance.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }, {})*/

                if (versionCheckServiceController.versionCheckSwitchTurnedOn)
                {
                    versionCheckServiceController.turnOnService()
                }else versionCheckServiceController.turnOffService()
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                println("OmniTrack application was updated.")
                if (versionCheckServiceController.versionCheckSwitchTurnedOn)
                {
                    versionCheckServiceController.turnOnService()
                }else versionCheckServiceController.turnOffService()

                //dispatcher.mustSchedule(informationUploadJobProvider.setTag(OTInformationUploadService.INFORMATION_DEVICE).build())

                /*
                OTApp.instance.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                    if (OTAuthManager.currentSignedInLevel > OTAuthManager.SignedInLevel.NONE) {
                        DatabaseManager.getDeviceInfoChild()?.child("appVersion")?.setValue(BuildConfig.VERSION_NAME)
                    }
                }, {})
                */
                //OTVersionCheckService.setupServiceAlarm(context)
            }

            Intent.ACTION_PACKAGE_ADDED -> {
                /*
                OTApp.instance.currentUserObservable.observeOn(Schedulers.immediate()).subscribe({
                    user ->
                    OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)
                    context.startService(OTShortcutPanelWidgetUpdateService.makeNotifyDatesetChangedIntentToAllWidgets(context))
                }, {})*/
            }
        }
    }
}