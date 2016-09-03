package kr.ac.snu.hcil.omnitrack.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created by Young-Ho on 9/3/2016.
 */
class OTShortcutManagerService: Service() {

    override fun onCreate() {
        super.onCreate()

    }

    override fun onBind(p0: Intent?): IBinder {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}