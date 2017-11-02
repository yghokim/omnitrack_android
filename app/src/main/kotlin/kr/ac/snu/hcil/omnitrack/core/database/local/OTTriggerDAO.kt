package kr.ac.snu.hcil.omnitrack.core.database.local

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTBackgroundLoggingTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTNotificationTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import javax.inject.Provider

/**
 * Created by Young-Ho on 10/9/2017.
 */
open class OTTriggerDAO : RealmObject() {

    enum class TriggerValidationComponent { TRACKER_ATTACHED, CONDITION_VALID }
    class TriggerConfigInvalidException(vararg _causes: TriggerValidationComponent) : Exception() {
        val causes: Array<out TriggerValidationComponent> = _causes

        override val message: String?
            get() = "trigger validation failed because [${causes.joinToString(", ")}] conditions were not met."
    }

    companion object {
        const val CONDITION_TYPE_TIME: Byte = 0
        const val CONDITION_TYPE_DATA: Byte = 1
        const val CONDITION_TYPE_EVENT: Byte = 2
        const val CONDITION_TYPE_ITEM: Byte = 3

        const val ACTION_TYPE_REMIND: Byte = 0
        const val ACTION_TYPE_LOG: Byte = 1
    }

    @PrimaryKey
    var objectId: String? = null
    var alias: String = ""
    var position: Int = 0

    @Index
    var userId: String? = null

    @Index
    var conditionType: Byte = CONDITION_TYPE_TIME
        set(value) {
            if (field != value) {
                field = value
                _condition = null
            }
        }

    var serializedCondition: String? = null
        set(value) {
            if (field != value) {
                field = value
                _condition = null
            }
        }

    @Ignore
    private var _condition: ATriggerCondition? = null
    val condition: ATriggerCondition?
        get() {
            if (_condition == null) {
                _condition = when (conditionType) {
                    CONDITION_TYPE_TIME -> serializedCondition?.let { OTTimeTriggerCondition.parser.fromJson(it, OTTimeTriggerCondition::class.java) } ?: OTTimeTriggerCondition()
                    else -> null
                }
            }
            return _condition
        }

    fun saveCondition(): Boolean {
        val serialized = _condition?.getSerializedString()
        if (serializedCondition != serialized) {
            serializedCondition = serialized
            return true
        } else return false
    }

    @Index
    var actionType: Byte = ACTION_TYPE_REMIND
        set(value) {
            if (field != value) {
                field = value
                _action = null
            }
        }
    var serializedAction: String? = null
        set(value) {
            if (field != value) {
                field = value
                _action = null
            }
        }

    @Ignore
    private var _action: OTTriggerAction? = null
    val action: OTTriggerAction?
        get() {
            if (_action == null) {
                _action = if (serializedAction == null) {
                    when (actionType) {
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction(this)
                        ACTION_TYPE_REMIND -> OTNotificationTriggerAction().apply { trigger = this@OTTriggerDAO }
                        else -> null
                    }
                } else {
                    when (actionType) {
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction(this)
                        ACTION_TYPE_REMIND -> OTNotificationTriggerAction.parser.fromJson(serializedAction, OTNotificationTriggerAction::class.java).apply { trigger = this@OTTriggerDAO }
                        else -> null
                    }
                }
            }

            return _action
        }

    fun saveAction(): Boolean =
            _action?.getSerializedString()?.let {
                if (serializedAction != it) {
                    serializedAction = it; true
                } else false
            } == true

    //Device-only properties===========
    //When synchronizing them, convey them with corresponding device local ids.
    var lastTriggeredTime: Long? = null
    var isOn: Boolean = false
    //=================================

    var trackers = RealmList<OTTrackerDAO>()

    val liveTrackersQuery: RealmQuery<OTTrackerDAO> get() = trackers.where().equalTo(RealmDatabaseManager.FIELD_REMOVED_BOOLEAN, false)
    val liveTrackerCount: Int get() = trackers.filter { it.removed == false }.size

    var synchronizedAt: Long? = null
    var removed: Boolean = false
    var updatedAt: Long = System.currentTimeMillis()
    var userCreatedAt: Long = System.currentTimeMillis()

    fun initialize(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            _action = null
            _condition = null
        }

        val action = this.action
        val condition = this.condition
        if (this.realm?.isInTransaction == false) {
            this.realm?.executeTransaction {
                saveAction()
                saveCondition()
            }
        } else {
            saveAction()
            saveCondition()
        }
    }

    /**
     * Check whether the trigger is valid to be turned on the system.
     *
     * @return if null, is valid. is not, the trigger is invalid due to the returned exception.
     */
    fun isValidToTurnOn(): TriggerConfigInvalidException? {
        val containsTracker = liveTrackerCount > 0

        val isConditionValid = when (conditionType) {
            OTTriggerDAO.CONDITION_TYPE_DATA -> {
                true
            }
            else -> true
        }

        return if (containsTracker && isConditionValid) null else {
            val invalids = ArrayList<TriggerValidationComponent>()
            if (!containsTracker) {
                invalids.add(TriggerValidationComponent.TRACKER_ATTACHED)
            }
            if (!isConditionValid) {
                invalids.add(TriggerValidationComponent.CONDITION_VALID)
            }

            TriggerConfigInvalidException(*invalids.toTypedArray())
        }
    }
}