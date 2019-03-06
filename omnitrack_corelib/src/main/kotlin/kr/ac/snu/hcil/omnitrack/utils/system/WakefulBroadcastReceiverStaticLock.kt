package kr.ac.snu.hcil.omnitrack.utils.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.SparseArray

class WakefulBroadcastReceiverStaticLock {
    companion object {
        const val EXTRA_WAKELOCK_ID = "wakeLockId"
    }

    private val mActiveWakeLocks = SparseArray<PowerManager.WakeLock>()
    private var mNextId = 1

    fun startWakefulService(context: Context, intent: Intent): ComponentName? {
        synchronized(mActiveWakeLocks) {
            val id = mNextId
            mNextId++
            if (mNextId <= 0) {
                mNextId = 1
            }

            intent.putExtra(EXTRA_WAKELOCK_ID, id)
            val comp = context.startService(intent) ?: return null

            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "wake:" + comp.flattenToShortString())
            wl.setReferenceCounted(false)
            wl.acquire(100000) // remove timeout
            mActiveWakeLocks.put(id, wl)
            return comp
        }
    }

    fun completeWakefulIntent(intent: Intent): Boolean {
        val id = intent.getIntExtra(EXTRA_WAKELOCK_ID, 0)
        if (id == 0) {
            return false
        }
        synchronized(mActiveWakeLocks) {
            val wl = mActiveWakeLocks.get(id)
            if (wl != null) {
                wl.release()
                mActiveWakeLocks.remove(id)
                return true
            }
            // We return true whether or not we actually found the wake lock
            // the return code is defined to indicate whether the Intent contained
            // an identifier for a wake lock that it was supposed to match.
            // We just log a warning here if there is no wake lock found, which could
            // happen for example if this function is called twice on the same
            // intent or the process is killed and restarted before processing the intent.
            return true
        }
    }
}