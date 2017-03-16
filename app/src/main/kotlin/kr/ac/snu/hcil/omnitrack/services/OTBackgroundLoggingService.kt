package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItem
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.FirebaseDbHelper
import kr.ac.snu.hcil.omnitrack.utils.isInDozeMode
import rx.Observable


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

        fun startLoggingInService(context: Context, tracker: OTTracker, source: OTItem.LoggingSource) {

            context.startService(makeIntent(context, tracker, source))
        }

        fun log(context: Context, tracker: OTTracker, source: OTItem.LoggingSource, notify: Boolean = true): Observable<Int> {

            return Observable.create<Int> {
                subscriber ->
                val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)

                OTApplication.logger.writeSystemLog("start background logging of ${tracker.name}", TAG)
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    OTApplication.logger.writeSystemLog("idleMode: ${isInDozeMode()}", TAG)
                }

                setLoggingFlag(tracker, System.currentTimeMillis())
                sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED, tracker)


                builder.autoComplete().subscribe({}, {}, {
                    val item = builder.makeItem(source)
                    FirebaseDbHelper.saveItem(item, tracker) {
                        success ->
                        if (success) {
                            sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED, tracker, item.objectId!!, notify)
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
                })
            }
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            context.sendBroadcast(intent)
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker, itemObjectId: String, notify: Boolean) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_ITEM, itemObjectId)
            intent.putExtra(INTENT_EXTRA_NOTIFY, notify)
            context.sendBroadcast(intent)
        }

        fun makeIntent(context: Context, tracker: OTTracker, source: OTItem.LoggingSource): Intent
        {
            val intent = Intent(context, OTBackgroundLoggingService::class.java)
            intent.action = ACTION_LOG
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(INTENT_EXTRA_LOGGING_SOURCE, source.toString())
            return intent
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        //tracker_id
        if (intent != null) {
            val action = intent.action
            if (ACTION_LOG == action) {
                println("try background logging..")
                val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                handleLogging(trackerId, intent)
            }
        }
    }

    private fun handleLogging(trackerId: String, intent: Intent) {
        OTApplication.app.currentUserObservable.flatMap { user -> user.getTrackerObservable(trackerId) }.subscribe {
            tracker ->
            log(this, tracker, OTItem.LoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE)), true).subscribe()
        }
    }




}
