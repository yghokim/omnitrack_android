package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckWorker
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver : BroadcastReceiver() {

    @Inject
    lateinit var versionCheckController: OTVersionCheckWorker.Controller

    override fun onReceive(context: Context, intent: Intent) {
        val app = (context.applicationContext as OTAndroidApp)
        app.scheduledJobComponent.inject(this)

        println("package broadcast receiver - ${intent.action}")

        val authManager = app.applicationComponent.getAuthManager()
        if (authManager.isUserSignedIn()) {

            val reminderCommands = OTReminderCommands(context)
            val realm = app.applicationComponent.backendRealmFactory().get()
            reminderCommands.restoreReminderNotifications(realm).blockingAwait()
            realm.close()

            val triggerSystemManager = app.triggerSystemComponent.getTriggerSystemManager().get()
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

                if (versionCheckController.versionCheckSwitchTurnedOn)
                {
                    versionCheckController.checkVersionOneTime()
                } else versionCheckController.cancelVersionCheckingWork()
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                println("OmniTrack application was updated.")
                if (versionCheckController.versionCheckSwitchTurnedOn)
                {
                    versionCheckController.checkVersionOneTime()
                } else versionCheckController.cancelVersionCheckingWork()

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
                //OTVersionCheckWorker.setupServiceAlarm(context)
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