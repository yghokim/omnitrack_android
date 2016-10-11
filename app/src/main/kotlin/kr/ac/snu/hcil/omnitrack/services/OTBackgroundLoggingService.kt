package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker


/*
*
* this will need to be refactored to be a normal service to handle the long retrieval operation of external measure factory values.
 */
class OTBackgroundLoggingService : IntentService("OTBackgroundLoggingService") {

    enum class LoggingSource {
        Trigger, Shortcut
    }

    companion object {
        private val ACTION_LOG = "kr.ac.snu.hcil.omnitrack.services.action.LOG"

        private const val INTENT_EXTRA_LOGGING_SOURCE = "loggingSource"

        fun startLoggingInService(context: Context, tracker: OTTracker, source: LoggingSource) {

            context.startService(makeIntent(context, tracker, source))
        }

        fun startLoggingAsync(context: Context, tracker: OTTracker, source: LoggingSource, finished: ((success: Boolean) -> Unit)? = null) {
            val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)

            sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED, tracker)
            builder.autoCompleteAsync {
                val item = builder.makeItem()
                OTApplication.app.dbHelper.save(item, tracker)
                if (item.dbId != null) {
                    sendBroadcast(context, OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED, tracker, item.dbId!!)
                    finished?.invoke(true)
                } else {
                    finished?.invoke(false)
                }
            }
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            context.sendBroadcast(intent)
        }

        private fun sendBroadcast(context: Context, action: String, tracker: OTTracker, itemDbId: Long) {
            val intent = Intent(action)
            intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            intent.putExtra(OTApplication.INTENT_EXTRA_DB_ID_ITEM, itemDbId)
            context.sendBroadcast(intent)
        }

        fun makeIntent(context: Context, tracker: OTTracker, source: LoggingSource): Intent
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
        val tracker = OTApplication.app.currentUser[trackerId]
        if (tracker != null) {
            startLoggingAsync(this, tracker, LoggingSource.valueOf(intent.getStringExtra(INTENT_EXTRA_LOGGING_SOURCE)), null)
        }
    }




}
