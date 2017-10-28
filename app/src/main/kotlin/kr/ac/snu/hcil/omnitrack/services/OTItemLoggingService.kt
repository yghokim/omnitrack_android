package kr.ac.snu.hcil.omnitrack.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.local.OTItemBuilderDAO

/**
 * Created by Young-Ho on 10/17/2017.
 */
class OTItemLoggingService : Service() {

    companion object {
        private const val ACTION_LOG = "${BuildConfig.APPLICATION_ID}.services.action.LOG"
        private const val INTENT_EXTRA_LOGGING_SOURCE = "loggingSource"

        fun makeLoggingIntent(context: Context, loggingSource: ItemLoggingSource, vararg trackerIds: String): Intent {
            return Intent(context, OTItemLoggingService::class.java).apply {
                this.action = ACTION_LOG
                this.putExtra(INTENT_EXTRA_LOGGING_SOURCE, loggingSource.name)
                this.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY, trackerIds)
            }
        }
    }

    private lateinit var realm: Realm
    private val subscriptions = CompositeDisposable()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        realm = OTApp.instance.databaseManager.getRealmInstance()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
        realm.close()
        println("ItemLoggingService onDestroy")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            ACTION_LOG -> {
                val trackerIds = intent.getStringArrayExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY)
                val loggingSource = ItemLoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE))
                if (trackerIds?.size ?: 0 >= 1) // intent contains one or more trackers
                {
                    val singles = trackerIds.mapNotNull { trackerId ->
                        Single.defer {
                            val trackerDao = OTApp.instance.databaseManager.getTrackerQueryWithId(trackerId, realm).findFirst()
                            if (trackerDao != null) {
                                val builder = OTItemBuilderDAO()
                                builder.holderType = OTItemBuilderDAO.HOLDER_TYPE_SERVICE
                                builder.tracker = realm.copyFromRealm(trackerDao)
                                val wrapper = OTItemBuilderWrapperBase(builder, realm)


                                wrapper.makeAutoCompleteObservable(applyToBuilder = true)
                                        .ignoreElements().toSingleDefault(true).flatMap {
                                    val item = wrapper.saveToItem(null, loggingSource)
                                    OTApp.instance.databaseManager.saveItemObservable(item, true, null, realm)
                                }
                            } else null
                        }
                    }

                    subscriptions.add(
                            Single.merge(singles).doOnComplete {
                                stopSelf(startId)
                            }.subscribe { }
                    )
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }


}