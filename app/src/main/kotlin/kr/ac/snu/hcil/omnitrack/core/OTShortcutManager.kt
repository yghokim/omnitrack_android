package kr.ac.snu.hcil.omnitrack.core

import android.content.Intent
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by Young-Ho on 9/4/2016.
 */
object OTShortcutManager {

    fun notifyAppearanceChanged(tracker: OTTracker) {
        val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_TRACKER_INFO_CHANGED)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)
    }

    operator fun plusAssign(tracker: OTTracker)
    {
        val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_INCLUDE_TRACKER)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)
    }

    operator fun minusAssign(tracker: OTTracker)
    {
        val intent = Intent(OTApplication.BROADCAST_ACTION_SHORTCUT_EXCLUDE_TRACKER)
        intent.putExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, tracker.objectId)

        OTApplication.app.sendBroadcast(intent)
    }
}