package kr.ac.snu.hcil.omnitrack.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import java.util.*

/**
 * Created by younghokim on 2017-11-29.
 */

class OTBinaryLocalCacheService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        const val TAG = "OTBinaryLocalCacheService"

        const val PREFIX_ACTION = "${OTApp.PREFIX_ACTION}.item_media_cache"
        const val INTENT_ACTION_REQUEST_CACHE = "${PREFIX_ACTION}.request_cache"
        const val INTENT_ACTION_RESPONSE_CACHE_STATUS = "${PREFIX_ACTION}.response_cache_status"

        const val INTENT_EXTRA_SERVER_PATH = "${BuildConfig.APPLICATION_ID}.intent.extra.serverPath"
        const val INTENT_EXTRA_STATUS = "${BuildConfig.APPLICATION_ID}.intent.extra.status"

        const val CACHE_STATUS_LOADING = 0
        const val CACHE_STATUS_LOADED = 1
        const val CACHE_STATUS_ERROR = 2

        val intentFilter: IntentFilter by lazy {
            IntentFilter(INTENT_ACTION_RESPONSE_CACHE_STATUS)
        }

        fun makeRequestCacheIntent(context: Context, serverPath: String): Intent {
            return Intent(context, OTBinaryLocalCacheService::class.java).setAction(INTENT_ACTION_REQUEST_CACHE)
                    .putExtra(INTENT_EXTRA_SERVER_PATH, serverPath)
        }

        fun makeStatusResponseIntent(context: Context, serverPath: String, statusCode: Int, localUri: Uri?): Intent {
            return Intent(INTENT_ACTION_RESPONSE_CACHE_STATUS)
                    .putExtra(INTENT_EXTRA_SERVER_PATH, serverPath)
                    .putExtra(INTENT_EXTRA_STATUS, statusCode)
                    .run {
                        if (localUri != null) {
                            return this.setData(localUri)
                        } else return this
                    }
        }
    }

    abstract class AReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == INTENT_ACTION_RESPONSE_CACHE_STATUS) {
                onReceivedCacheResponse(intent.getIntExtra(INTENT_EXTRA_STATUS, 0), intent.data)
            }
        }

        protected abstract fun onReceivedCacheResponse(statusCode: Int, localUri: Uri?)
    }

    private val subscriptionDict = Hashtable<String, Disposable>()

    override fun onDestroy() {
        super.onDestroy()
        subscriptionDict.values.forEach { it.dispose() }
        subscriptionDict.clear()
    }
}