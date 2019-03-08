package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.content.Intent
import dagger.internal.Factory
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilderWrapperBase
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTItemDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTItemBuilderDAO
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTTaskNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationFactory
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Created by Young-Ho on 10/17/2017.
 */
class OTItemLoggingService : WakefulService(TAG) {

    companion object {

        const val TAG = "OTItemLoggingService"

        private const val ACTION_LOG = "${BuildConfig.APPLICATION_ID}.services.action.LOG"
        private const val ACTION_REMOVE_ITEM = "${BuildConfig.APPLICATION_ID}.services.action.REMOVE_ITEM"
        private const val INTENT_EXTRA_LOGGING_SOURCE = "loggingSource"
        private const val INTENT_EXTRA_NOTIFY = "notify"

        private const val NOTIFICATION_FOREGROUND_ID = 3123
        private const val NOTIFICATION_TAG = "${BuildConfig.APPLICATION_ID}.notification.tag.ITEM_LOGGING_SERVICE"
        private val notificationIdSeed = AtomicInteger(0)

        fun makeLoggingIntent(context: Context, loggingSource: ItemLoggingSource, notify: Boolean, vararg trackerIds: String): Intent {
            return Intent(context, OTItemLoggingService::class.java).apply {
                this.action = ACTION_LOG
                this.putExtra(INTENT_EXTRA_LOGGING_SOURCE, loggingSource.name)
                this.putExtra(INTENT_EXTRA_NOTIFY, notify)
                this.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY, trackerIds)
            }
        }

        fun makeRemoveItemIntent(context: Context, itemId: String): Intent {
            return Intent(context, OTItemLoggingService::class.java).apply {
                this.action = ACTION_REMOVE_ITEM
                this.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM, itemId)
            }
        }
    }

    lateinit var realm: Realm

    @field:[Inject Backend]
    lateinit var realmProvider: Factory<Realm>

    @Inject
    lateinit var dbManager: BackendDbManager

    @Inject
    lateinit var syncManager: OTSyncManager

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
        if (this::realm.isInitialized) {
            if (!realm.isClosed)
                realm.close()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return when (intent.action) {
            ACTION_LOG -> handleLogAction(intent, startId)
            ACTION_REMOVE_ITEM -> handleRemoveAction(intent, startId)
            else -> START_NOT_STICKY
        }
    }

    private fun handleLogAction(intent: Intent, startId: Int): Int {
        realm = realmProvider.get()
        val trackerIds = intent.getStringArrayExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER_ARRAY)
        val loggingSource = ItemLoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE))
        val notify = intent.getBooleanExtra(INTENT_EXTRA_NOTIFY, true)
        if (trackerIds?.size ?: 0 >= 1) // intent contains one or more trackers
        {
            val singles = trackerIds.mapNotNull { trackerId ->
                Single.defer {
                    val trackerDao = dbManager.getTrackerQueryWithId(trackerId, realm).findFirst()
                    if (trackerDao != null) {
                        val unManagedTrackerDao = realm.copyFromRealm(trackerDao)
                        val builder = OTItemBuilderDAO()
                        builder.holderType = OTItemBuilderDAO.HOLDER_TYPE_SERVICE
                        builder.tracker = unManagedTrackerDao
                        val wrapper = OTItemBuilderWrapperBase(builder, this)
                        val trackerName = trackerDao.name
                        val notificationId = OTItemLoggingService.notificationIdSeed.incrementAndGet()

                        var pushedItemDao: OTItemDAO? = null

                        wrapper.makeAutoCompleteObservable(realmProvider, applyToBuilder = true)
                                .ignoreElements().toSingleDefault(trackerId).flatMap {
                                    //TODO put metadata
                                    val item = wrapper.saveToItem(null, loggingSource, null)
                                    pushedItemDao = item
                                    dbManager.saveItemObservable(item, true, null, realm)
                                }.doOnSubscribe {
                                    this@OTItemLoggingService.runOnUiThread {
                                        OTTaskNotificationManager
                                                .setTaskProgressNotification(this, NOTIFICATION_TAG,
                                                        notificationId,
                                                        "Logging...",
                                                        "Logging $trackerName...",
                                                        OTTaskNotificationManager.PROGRESS_INDETERMINATE,
                                                        null
                                                )
                                    }
                                }.doOnSuccess { (result, itemId) ->
                                    if (result != BackendDbManager.SAVE_RESULT_FAIL) {
                                        val table = ArrayList<Pair<String, CharSequence?>>()
                                        val item = pushedItemDao
                                        if (item != null && unManagedTrackerDao != null) {
                                            unManagedTrackerDao.attributes.filter { !it.isHidden && !it.isInTrashcan }.forEach {
                                                val value = item.getValueOf(it.localId)
                                                if (value != null) {
                                                    table.add(Pair(it.name, it.getHelper(this).formatAttributeValue(it, value)))
                                                } else {
                                                    table.add(Pair(it.name, null))
                                                }
                                            }
                                        }

                                        println(table)

                                        syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)

                                        if (notify) {
                                            this@OTItemLoggingService.runOnUiThread {
                                                val successfulNotiBuilder = OTTrackingNotificationFactory.makeLoggingSuccessNotificationBuilder(this@OTItemLoggingService, trackerId, trackerName, itemId!!, System.currentTimeMillis(), table, notificationId, NOTIFICATION_TAG)

                                                this@OTItemLoggingService.notificationManager.notify(kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService.NOTIFICATION_TAG, notificationId, successfulNotiBuilder.build())
                                            }
                                        }
                                    } else {
                                        OTTaskNotificationManager.dismissNotification(this@OTItemLoggingService, notificationId, kr.ac.snu.hcil.omnitrack.services.OTItemLoggingService.NOTIFICATION_TAG)
                                    }
                                }
                    } else null
                }
            }

            subscriptions.add(
                    Single.merge(singles).doOnComplete {
                        this.stopSelf(startId)
                    }.subscribe { }
            )
            return START_REDELIVER_INTENT
        } else {
            stopSelf(startId)
            return START_NOT_STICKY
        }
    }

    private fun handleRemoveAction(intent: Intent, startId: Int): Int {
        realm = realmProvider.get()
        val itemId = intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_ITEM)
        if (!itemId.isNullOrBlank()) {
            val item = dbManager.makeSingleItemQuery(itemId, realm).findFirst()
            if (item != null) {
                realm.executeTransaction {
                    dbManager.removeItem(item, false, realm)
                }
                syncManager.registerSyncQueue(ESyncDataType.ITEM, SyncDirection.UPLOAD, ignoreDirtyFlags = false)

                if (intent.hasExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID)) {
                    val notiId = intent.getIntExtra(OTApp.INTENT_EXTRA_NOTIFICATION_ID, -100)
                    val notiTag = intent.getStringExtra(OTApp.INTENT_EXTRA_NOTIFICATON_TAG)
                    if (!notiTag.isNullOrBlank()) {
                        this@OTItemLoggingService.notificationManager.cancel(notiTag, notiId)
                    } else {
                        this@OTItemLoggingService.notificationManager.cancel(notiId)
                    }
                }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}