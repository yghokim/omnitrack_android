package kr.ac.snu.hcil.omnitrack.core.flags

import com.google.gson.JsonObject
import kr.ac.snu.hcil.android.common.containers.TwoKeyDictionary
import kr.ac.snu.hcil.omnitrack.core.serialization.getBooleanCompat

/**
 * Created by Young-Ho on 1/19/2018.
 */
object LockedPropertiesHelper : AFlagsHelperBase() {

    private val defaultValueDict = TwoKeyDictionary<String, String, Boolean>()

    init {
        defaultValueDict.put(LockFlagLevel.App, F.ModifyExistingTrackersTriggers, false)
        defaultValueDict.put(LockFlagLevel.App, F.AddNewTracker, false)
        defaultValueDict.put(LockFlagLevel.App, F.AccessTriggersTab, false)
        defaultValueDict.put(LockFlagLevel.App, F.AddNewTrigger, false)
        defaultValueDict.put(LockFlagLevel.App, F.AccessServicesTab, false)
        defaultValueDict.put(LockFlagLevel.App, F.UseShortcutPanel, true)
        defaultValueDict.put(LockFlagLevel.App, F.UseScreenWidget, true)

        defaultValueDict.put(LockFlagLevel.Tracker, F.Visible, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.AccessItems, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.ModifyItems, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.AccessVisualization, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.ManualInput, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.Modify, false)
        defaultValueDict.put(LockFlagLevel.Tracker, F.Delete, false)
        defaultValueDict.put(LockFlagLevel.Tracker, F.ToggleShortcut, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.AddNewFields, false)
        defaultValueDict.put(LockFlagLevel.Tracker, F.ModifyReminders, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.AddNewReminders, true)
        defaultValueDict.put(LockFlagLevel.Tracker, F.EditName, false)
        defaultValueDict.put(LockFlagLevel.Tracker, F.EditColor, false)
        defaultValueDict.put(LockFlagLevel.Tracker, F.ReorderFields, false)

        defaultValueDict.put(LockFlagLevel.Field, F.Modify, false)
        defaultValueDict.put(LockFlagLevel.Field, F.Delete, false)
        defaultValueDict.put(LockFlagLevel.Field, F.ToggleVisibility, true)
        defaultValueDict.put(LockFlagLevel.Field, F.EditProperties, true)
        defaultValueDict.put(LockFlagLevel.Field, F.EditMeasureFactory, false)
        defaultValueDict.put(LockFlagLevel.Field, F.ToggleRequired, false)
        defaultValueDict.put(LockFlagLevel.Field, F.EditName, false)

        defaultValueDict.put(LockFlagLevel.Reminder, F.Visible, true)
        defaultValueDict.put(LockFlagLevel.Reminder, F.Modify, false)
        defaultValueDict.put(LockFlagLevel.Reminder, F.Delete, false)
        defaultValueDict.put(LockFlagLevel.Reminder, F.ToggleSwitch, true)
        defaultValueDict.put(LockFlagLevel.Reminder, F.EditProperties, true)


        defaultValueDict.put(LockFlagLevel.Trigger, F.Visible, true)
        defaultValueDict.put(LockFlagLevel.Trigger, F.Modify, false)
        defaultValueDict.put(LockFlagLevel.Trigger, F.Delete, false)
        defaultValueDict.put(LockFlagLevel.Trigger, F.ToggleSwitch, true)
        defaultValueDict.put(LockFlagLevel.Trigger, F.ModifyAssignedTrackers, true)
        defaultValueDict.put(LockFlagLevel.Trigger, F.EditProperties, true)
    }

    fun getDefaultValue(level: String, flag: String): Boolean {
        return defaultValueDict.get(level, flag)!!
    }

    fun flag(level: String, flag: String, properties: JsonObject?): Boolean {
        return if (properties == null) {
            getDefaultValue(level, flag)
        } else {
            if (properties.has(flag)) {
                properties.getBooleanCompat(flag)!!
            } else getDefaultValue(level, flag)
        }
    }

    fun generateDefaultFlags(level: String, fillWith: Boolean?): JsonObject {
        val obj = JsonObject()
        val map = defaultValueDict.getWithFirstKey(level)
        map?.keys?.forEach { flag ->
            obj.addProperty(flag, fillWith ?: map[flag])
        }

        return obj
    }
}