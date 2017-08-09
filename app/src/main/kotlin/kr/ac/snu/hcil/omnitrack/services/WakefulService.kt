package kr.ac.snu.hcil.omnitrack.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager

/**
 * Created by younghokim on 2017. 3. 14..
 */
open class WakefulService(val tag: String) : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)
        if (!(wakeLock?.isHeld ?: false)) {
            wakeLock?.acquire()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (wakeLock?.isHeld ?: false) {
            wakeLock?.release()
        }
    }
}