package kr.ac.snu.hcil.omnitrack.services

import android.app.IntentService
import android.content.Intent
import android.content.Context
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTracker


/*TODO
*
* this will need to be refactored to be a normal service to handle the long retrieval operation of external measure factory values.
 */
class OTBackgroundLoggingService : IntentService("OTBackgroundLoggingService") {

    companion object {
        private val ACTION_LOG = "kr.ac.snu.hcil.omnitrack.services.action.LOG"

        fun startLogging(context: Context, tracker: OTTracker) {
            val intent = Intent(context, OTBackgroundLoggingService::class.java)
            intent.action = ACTION_LOG
            intent.putExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        //tracker_id
        if (intent != null) {
            val action = intent.action
            if (ACTION_LOG == action) {
                val trackerId = intent.getStringExtra(OmniTrackApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
                handleLogging(trackerId)
            }
        }
    }

    private fun handleLogging(trackerId: String) {
        val tracker = OmniTrackApplication.app.currentUser[trackerId]
        if (tracker != null) {
            val builder = OTItemBuilder(tracker, OTItemBuilder.MODE_BACKGROUND)
            OmniTrackApplication.app.dbHelper.save(builder.makeItem(), tracker)
        }
    }
}
