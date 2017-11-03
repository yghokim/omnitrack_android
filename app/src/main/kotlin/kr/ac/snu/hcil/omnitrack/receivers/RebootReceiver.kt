package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.system.OTShortcutPanelManager
import kr.ac.snu.hcil.omnitrack.services.OTVersionCheckService

/**
 * Created by Young-Ho on 9/4/2016.
 */
class RebootReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "RebootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        println("OMNITRACK: reboot receiver called - ${intent.action}")

        val result = goAsync()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wl.acquire(3000)

        result.finish()
        wl.release()

        OTApp.instance.timeTriggerAlarmManager.activateOnSystem()

        /*
        OTApp.instance.currentUserObservable.observeOn(Schedulers.newThread()).subscribe({
            user ->
            OTShortcutPanelManager.refreshNotificationShortcutViews(user, context)

        }, {}, {
            result.finish()
            wl.release()
            println("OMNITRACK: reboot receiver finished.")
        })*/

    }

}