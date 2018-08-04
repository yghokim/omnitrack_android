package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTriggerSystemManager
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by Young-Ho on 9/4/2016.
 */
class RebootReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "RebootReceiver"
    }

    @Inject
    protected lateinit var triggerManager: OTTriggerSystemManager

    @Inject
    protected lateinit var shortcutPanelManager: OTShortcutPanelManager

    @Inject
    protected lateinit var eventLogger: Lazy<IEventLogger>

    override fun onReceive(context: Context, intent: Intent) {
        println("OMNITRACK: reboot receiver called - ${intent.action}")

        (context.applicationContext as OTApp).currentConfiguredContext.configuredAppComponent.inject(this)

        val result = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wl.acquire(3000)

        eventLogger.get().logEvent(IEventLogger.NAME_DEVICE_EVENT, "AfterBoot")

        triggerManager.onSystemRebooted()

        WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<OTReminderService.SystemRebootWorker>()
                .build())

        shortcutPanelManager.refreshNotificationShortcutViewsObservable(context).timeout(2, TimeUnit.SECONDS).doAfterTerminate {
            result.finish()
            wl.release()
            println("OMNITRACK: reboot receiver finished.")
        }.subscribe({

        }, { err ->
            err.printStackTrace()
        })


        /*
        OTApp.instance.currentUserObservable.observeOn(Schedulers.newThread()).subscribe({
            user ->
            OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)

        }, {}, {

        })*/

    }

}