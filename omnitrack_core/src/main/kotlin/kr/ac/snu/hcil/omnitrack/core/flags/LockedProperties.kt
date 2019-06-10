package kr.ac.snu.hcil.omnitrack.core.flags

object LockFlagLevel {

    const val Field = "field"
    const val Reminder = "reminder"
    const val Trigger = "trigger"
    const val Tracker = "tracker"
    const val App = "app"
}

object F {

    //AppLevel
    const val ModifyExistingTrackersTriggers = "modifyTrTg"
    const val AddNewTracker = "addNewTracker"
    const val AccessTriggersTab = "accessTriggersTab"
    const val AddNewTrigger = "addNewTrigger"
    const val AccessServicesTab = "accessServicesTab"
    const val UseShortcutPanel = "useShortcut"
    const val UseScreenWidget = "useWidget"

    //entities common
    const val Visible = "visible"
    const val Modify = "modify"
    const val Delete = "delete"
    const val EditName = "editName" // tracker fields
    const val EditProperties = "editProp" // triggers reminders

    //Tracker Level
    const val AccessItems = "accessItems"
    const val ModifyItems = "modifyItems"
    const val AccessVisualization = "accessVis"
    const val ManualInput = "manualInput"
    const val ToggleShortcut = "toggleShortcut"
    const val ModifyFields = "modifyFields"
    const val AddNewFields = "addNewField"
    const val ModifyReminders = "modifyRem"
    const val AddNewReminders = "addNewRem"
    const val EditColor = "editColor"
    const val ReorderFields = "reorderFields"

    //Field Level
    const val ToggleVisibility = "toggleVisible"
    const val EditMeasureFactory = "editMeasure"
    const val ToggleRequired = "toggleRequired"

    //Trigger/reminder
    const val ToggleSwitch = "switch"
    const val ModifyAssignedTrackers = "modifyAssignees"
}

