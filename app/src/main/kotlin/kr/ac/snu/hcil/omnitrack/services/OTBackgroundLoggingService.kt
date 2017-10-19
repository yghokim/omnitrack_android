package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


/*
*
* this will need to be refactored to be a normal service to handle the long retrieval operation of external measure factory values.
 */
class OTBackgroundLoggingService : IntentService("OTBackgroundLoggingService") {

    companion object {
        const val TAG = "BGLoggingService"

        private val ACTION_LOG = "kr.ac.snu.hcil.omnitrack.services.action.LOG"


        private const val INTENT_EXTRA_LOGGING_SOURCE = "loggingSource"
        const val INTENT_EXTRA_NOTIFY = "logging_sendNotification"

        private val notificationIdSeed = AtomicInteger(0)

        private fun makeNewNotificationIdSeed(): Int {
            return notificationIdSeed.addAndGet(1)
        }

        private val flagPreferences: SharedPreferences by lazy {
            OTApplication.app.getSharedPreferences("pref_background_logging_service", Context.MODE_PRIVATE)
        }

        private fun getLoggingFlag(tracker: OTTracker): Long? {
            return if (flagPreferences.contains(tracker.objectId)) {
                flagPreferences.getLong(tracker.objectId, Long.MIN_VALUE)
            } else null
        }

        /**
         * return: String: tracker id, Long: Timestamp
         */
        fun getFlags(): List<Pair<String, Long>> {
            return flagPreferences.all.entries.map { Pair(it.key, it.value as Long) }
        }

        private fun setLoggingFlag(tracker: OTTracker, timestamp: Long) {
            flagPreferences.edit().putLong(tracker.objectId, timestamp).apply()
        }

        private fun removeLoggingFlag(tracker: OTTracker) {
            flagPreferences.edit().remove(tracker.objectId).apply()
        }

        private val currentBuilderSubscriptionDict = ConcurrentHashMap<String, Disposable>()

        fun cancelBuilderTask(trackerId: String): Boolean {
            return currentBuilderSubscriptionDict.remove(trackerId) != null
        }

        fun startLoggingInService(context: Context, tracker: OTTracker, source: ItemLoggingSource) {

            context.startService(makeIntent(context, tracker, source))
        }

        fun log(context: Context, tracker: OTTracker, source: ItemLoggingSource, notify: Boolean = true): Observable<Int> {

            return Observable.create<Int> { subscriber ->
                /*
                val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)

                OTApplication.logger.writeSystemLog("start background logging of ${tracker.name}", TAG)
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    OTApplication.logger.writeSystemLog("idleMode: ${isInDozeMode()}", TAG)
                }

                setLoggingFlag(tracker, System.currentTimeMillis())
                val notificationId = makeNewNotificationIdSeed()
                sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED, tracker, notificationId)


                if (currentBuilderSubscriptionDict.contains(tracker.objectId)) {
                    currentBuilderSubscriptionDict[tracker.objectId]?.unsubscribe()
                    currentBuilderSubscriptionDict.remove(tracker.objectId)
                } else {
                    currentBuilderSubscriptionDict[tracker.objectId] = builder.autoComplete().last().toSingle().map { pair -> true }.doOnSuccess { currentBuilderSubscriptionDict.remove(tracker.objectId) }.flatMap<Pair<Boolean, OTItem>> {
                        val item = builder.makeItem(source)
                        //OTApplication.app.databaseManager.saveItemObservable(item, tracker).map { success -> Pair(success, item) }
                        //TODO fix item save logic in background logging
                        Single.just(Pair(false, item))

                    }.subscribe { successItemPair ->
                        if (successItemPair.first == true) {
                            sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED, tracker, successItemPair.second.objectId!!, notify, notificationId)
                            OTApplication.logger.writeSystemLog("${tracker.name} background logging was successful", TAG)
                            removeLoggingFlag(tracker)
                            if (!subscriber.isUnsubscribed) {
                                subscriber.onNext(0)
                                subscriber.onCompleted()
                            }
                        } else {

                            OTApplication.logger.writeSystemLog("${tracker.name} background logging failed", TAG)
                            removeLoggingFlag(tracker)
                            if (!subscriber.isUnsubscribed) {
                                subscriber.onError(Exception("Item storing failed"))
                            }
                        }
                    }
                }
                */
            }
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker, notificationIdSeed: Int) {
            val intent = Intent(action)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    .putExtra(OTApplication.INTENT_EXTRA_NOTIFICATION_ID_SEED, notificationIdSeed)
            context.sendBroadcast(intent)
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker, itemObjectId: String, notify: Boolean, notificationIdSeed: Int) {
            val intent = Intent(action)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
                    .putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemObjectId)
                    .putExtra(OTApplication.INTENT_EXTRA_NOTIFICATION_ID_SEED, notificationIdSeed)
                    .putExtra(INTENT_EXTRA_NOTIFY, notify)
            context.sendBroadcast(intent)
        }

        fun makeIntent(context: Context, tracker: OTTracker, source: ItemLoggingSource): Intent {
            return makeIntent(context, tracker.objectId, source)
        }

        fun makeIntent(context: Context, trackerId: String, source: ItemLoggingSource): Intent {
            val intent = Intent(context, OTBackgroundLoggingService::class.java)
            intent.action = "${ACTION_LOG}${trackerId}"
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
            intent.putExtra(INTENT_EXTRA_LOGGING_SOURCE, source.toString())
            return intent
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        //tracker_id
        if (intent != null) {
            val action = intent.action
            if (action.startsWith(ACTION_LOG, true)) {
                println("try background logging..")
                val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                handleLogging(trackerId, intent)
            }
        }
    }

    private fun handleLogging(trackerId: String, intent: Intent) {
        /*
        OTApplication.app.currentUserObservable.flatMap { user -> user.getTrackerObservable(trackerId) }.subscribe { tracker ->
            log(this, tracker, ItemLoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE)), true).subscribe()
        }*/
    }


}
