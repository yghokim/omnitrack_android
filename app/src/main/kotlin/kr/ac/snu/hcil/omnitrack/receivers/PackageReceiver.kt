package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.di.Backend
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.core.workers.OTVersionCheckWorker
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver : BroadcastReceiver() {

    @Inject
    lateinit var versionCheckController: OTVersionCheckWorker.Controller

    @Inject
    lateinit var triggerSystemManager: Lazy<OTTriggerSystemManager>

    @Inject
    lateinit var authManager: Lazy<OTAuthManager>

    @field:[Inject Backend]
    lateinit var realmFactory: Factory<Realm>

    override fun onReceive(context: Context, intent: Intent) {
        val app = (context.applicationContext as OTAndroidApp)
        app.applicationComponent.inject(this)

        println("package broadcast receiver - ${intent.action}")

        if (authManager.get().isUserSignedIn()) {

            val reminderCommands = OTReminderCommands(context)
            val realm = realmFactory.get()
            reminderCommands.restoreReminderNotifications(realm).blockingAwait()
            realm.close()

            triggerSystemManager.get().checkInAllToSystem(authManager.get().userId!!)
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