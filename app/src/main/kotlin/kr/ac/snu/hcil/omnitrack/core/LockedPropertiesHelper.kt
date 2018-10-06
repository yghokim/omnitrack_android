package kr.ac.snu.hcil.omnitrack.core

import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.utils.getBooleanCompat

/**
 * Created by Young-Ho on 1/19/2018.
 */
object LockedPropertiesHelper : AFlagsHelperBase() {
    const val COMMON_DELETE = "delete"
    const val COMMON_EDIT = "edit"
    const val TRACKER_BOOKMARK = "bookmark"
    const val TRACKER_ADD_NEW_ATTRIBUTE = "addNewAttribute"
    const val TRACKER_REMOVE_ATTRIBUTES = "removeAttributes"
    const val TRACKER_EDIT_ATTRIBUTES = "editAttributes"
    const val TRACKER_CHANGE_NAME = "changeName"
    const val TRACKER_CHANGE_ATTRIBUTE_ORDER = "changeAttributeOrder"
    const val TRACKER_ADD_NEW_REMINDER = "addNewReminder"
    const val TRACKER_SELF_INITIATED_INPUT = "selfInitiatedInput"
    const val TRACKER_VISIBLE_IN_APP = "visibleInApp"

    const val TRACKER_ENTER_ITEM_LIST = "enterItemList"
    const val TRACKER_ENTER_VISUALIZATION = "enterVisualization"
    const val TRIGGER_CHANGE_ASSIGNED_TRACKERS = "changeAssignedTrackers"
    const val TRIGGER_CHANGE_SWITCH = "changeSwitch"
    const val TRIGGER_VISIBLE_IN_APP = "visibleInApp"

    const val ATTRIBUTE_VISIBILITY = "visibility"

    fun isLocked(key: String, properties: JsonObject?): Boolean? {
        return properties?.getBooleanCompat(key)
    }

    fun isLockedNotNull(key: String, properties: JsonObject?): Boolean {
        return properties?.getBooleanCompat(key) ?: false
    }


    class Builder : BuilderBase() {
        fun setLocked(key: String, isLocked: Boolean?): Builder {
            this.json.set(key, isLocked)
            return this
        }
    }
}