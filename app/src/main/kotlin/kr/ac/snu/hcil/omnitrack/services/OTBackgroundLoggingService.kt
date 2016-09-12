package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker


/*TODO
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

        fun startLogging(context: Context, tracker: OTTracker, source: LoggingSource) {

            context.startService(makeIntent(context, tracker, source))
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
                val trackerId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                handleLogging(trackerId)
            }
        }
    }

    private fun handleLogging(trackerId: String) {
        val tracker = OTApplication.app.currentUser[trackerId]
        if (tracker != null) {
            val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)

            sendBroadcast(OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_STARTED, tracker)

            builder.autoCompleteAsync {
                OTApplication.app.dbHelper.save(builder.makeItem(), tracker)
                sendBroadcast(OTApplication.BROADCAST_ACTION_BACKGROUND_LOGGING_SUCCEEDED, tracker)
            }
        }
    }

    private fun sendBroadcast(action: String, tracker: OTTracker) {
        val intent = Intent(action)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
        sendBroadcast(intent)
    }
}
