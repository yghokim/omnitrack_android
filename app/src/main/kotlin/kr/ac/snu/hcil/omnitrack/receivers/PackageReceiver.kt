package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.di.InformationUpload
import kr.ac.snu.hcil.omnitrack.services.OTInformationUploadService
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/4/2016.
 */
class PackageReceiver : BroadcastReceiver() {

    @Inject lateinit var versionCheckServiceController: OTVersionCheckService.Controller

    @field:[Inject InformationUpload]
    lateinit var informationUploadJobProvider: Job.Builder
    @Inject lateinit var dispatcher: FirebaseJobDispatcher

    override fun onReceive(context: Context, intent: Intent) {

        (context.applicationContext as OTApp).scheduledJobComponent.inject(this)

        println("package broadcast receiver")
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

                dispatcher.mustSchedule(informationUploadJobProvider.setTag(OTInformationUploadService.INFORMATION_DEVICE).build())

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