package kr.ac.snu.hcil.omnitrack.core.database

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.backend.OTAuthManager
import java.util.*

/**
 * Created by Young-Ho on 3/1/2017.
 */

object EventLoggingManager {

    const val CHILD_NAME_EVENTS = "events"
    const val CHILD_NAME_ANONYMOUS = "anonymous"

    const val EVENT_NAME_CHANGE_ATTRIBUTE_ADD = "change_field_add"
    const val EVENT_NAME_CHANGE_ATTRIBUTE_REMOVE = "change_field_remove"
    const val EVENT_NAME_CHANGE_ATTRIBUTE_ORDER = "change_field_order"

    const val EVENT_NAME_CHANGE_TRACKER_ADD = "change_tracker_add"
    const val EVENT_NAME_CHANGE_TRACKER_REMOVE = "change_tracker_remove"
    const val EVENT_NAME_CHANGE_TRACKER_NAME = "change_tracker_name"
    const val EVENT_NAME_CHANGE_TRACKER_ON_SHORTCUT = "change_tracker_on_shortcut"


    const val EVENT_NAME_CHANGE_TRIGGER_ADD = "change_trigger_add"
    const val EVENT_NAME_CHANGE_TRIGGER_REMOVE = "change_trigger_remove"
    const val EVENT_NAME_CHANGE_TRIGGER_SWITCH = "change_trigger_switch"

    private val analytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(OTApplication.app)
    }

    fun logEvent(name: String, params: Bundle) {
        analytics.logEvent(name, params)

        val table = HashMap<String, Any>()
        table["event_name"] = name
        for (key in params.keySet()) {
            try {
                table[key] = params.get(key)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }

        table["timestamp"] = System.currentTimeMillis()
        table["app_id"] = FirebaseDatabase.getInstance().app.options.applicationId

        FirebaseHelper.dbRef?.child(CHILD_NAME_EVENTS)?.child(OTAuthManager.userId ?: CHILD_NAME_ANONYMOUS)?.push()?.setValue(table)
    }

    fun logAttributeChangeEvent(name: String, attributeType: Int, attributeId: String, trackerId: String) {
        val bundle = Bundle()
        bundle.putInt("attr_type", attributeType)
        bundle.putString("attr_id", attributeId)
        bundle.putString("tracker_id", trackerId)
        logEvent(name, bundle)
    }

    fun logTrackerChangeEvent(name: String, tracker: OTTracker) {
        logEvent(name, makeTrackerChangeEventParams(tracker))
    }

    fun makeTrackerChangeEventParams(tracker: OTTracker): Bundle {
        return Bundle().apply {
            putString("tracker_id", tracker.objectId)
            putString("tracker_name", tracker.name)
        }
    }

    fun logTrackerOnShortcutChangeEvent(tracker: OTTracker, isOnShortcut: Boolean) {
        logEvent(EVENT_NAME_CHANGE_TRACKER_ON_SHORTCUT, makeTrackerChangeEventParams(tracker).apply { putBoolean("on_shortcut", isOnShortcut) })
    }

    fun logTriggerChangeEvent(name: String, triggerId: String, type: Int, action: Int) {
        logEvent(name, makeTriggerChangeEventParams(triggerId, type, action))
    }

    fun makeTriggerChangeEventParams(triggerId: String, type: Int, action: Int): Bundle {
        return Bundle().apply {
            putString("trigger_id", triggerId)
            putInt("trigger_type", type)
            putInt("trigger_action", action)
        }
    }
}