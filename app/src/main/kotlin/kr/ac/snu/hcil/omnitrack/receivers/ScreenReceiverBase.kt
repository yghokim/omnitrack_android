package kr.ac.snu.hcil.omnitrack.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * Created by younghokim on 2017. 4. 20..
 */
open class ScreenReceiverBase : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action

        println(action)

        when (action) {
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
            Intent.ACTION_SCREEN_ON -> onScreenOn()
        }
    }

    protected open fun onScreenOn() {

    }

    protected open fun onScreenOff() {

    }

    companion object {
        val filter: IntentFilter by lazy {
            IntentFilter().apply {
                this.addAction(Intent.ACTION_SCREEN_ON)
                this.addAction(Intent.ACTION_SCREEN_OFF)
            }
        }
    }


}