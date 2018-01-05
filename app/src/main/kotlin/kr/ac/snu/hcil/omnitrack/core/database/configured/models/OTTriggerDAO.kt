package kr.ac.snu.hcil.omnitrack.core.database.configured.models

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import io.reactivex.Completable
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionEvaluator
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTBackgroundLoggingTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import org.jetbrains.anko.runOnUiThread

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

        const val ACTION_TYPE_REMIND: Byte = 0
        const val ACTION_TYPE_LOG: Byte = 1
    }

    @PrimaryKey
    var objectId: String? = null
    var alias: String = ""
    var position: Int = 0

    var serializedLockedPropertyInfo: String = "{}"

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
                    CONDITION_TYPE_TIME -> serializedCondition?.let { OTTimeTriggerCondition.typeAdapter.fromJson(it) } ?: OTTimeTriggerCondition()
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
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction().apply { trigger = this@OTTriggerDAO }
                        ACTION_TYPE_REMIND -> OTReminderAction().apply { trigger = this@OTTriggerDAO }
                        else -> null
                    }
                } else {
                    when (actionType) {
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction.typeAdapter.fromJson(serializedAction).apply { trigger = this@OTTriggerDAO }
                        ACTION_TYPE_REMIND -> OTReminderAction.typeAdapter.fromJson(serializedAction).apply { trigger = this@OTTriggerDAO }
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
    var isOn: Boolean = false
    //=================================

    var additionalScript: String? = null
    var checkScript: Boolean = false

    var trackers = RealmList<OTTrackerDAO>()

    val liveTrackersQuery: RealmQuery<OTTrackerDAO> get() = trackers.where().equalTo(BackendDbManager.FIELD_REMOVED_BOOLEAN, false)
    val liveTrackerCount: Int
        get() {
            return if (this.isManaged) {
                liveTrackersQuery.findAll().count()
            } else trackers.filter { !it.removed }.size
        }

    var synchronizedAt: Long? = null

    @Index
    var removed: Boolean = false
    var userUpdatedAt: Long = System.currentTimeMillis()
    var userCreatedAt: Long = System.currentTimeMillis()

    var serializedCreationFlags: String = "{}"

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

    fun validateScriptIfExist(configuredContext: ConfiguredContext): Boolean {
        if (checkScript) {
            if (additionalScript == null || additionalScript?.contentEquals("TRUE") == true) {
                return true
            } else {
                //evaluate
                val evaluator = ExpressionEvaluator(additionalScript!!, *configuredContext.configuredAppComponent.getSupportedScriptFunctions())
                return evaluator.evalBoolean()
            }
        } else {
            return true
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
            CONDITION_TYPE_DATA -> {
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

    fun performFire(triggerTime: Long, configuredContext: ConfiguredContext): Completable {
        val triggerId = objectId
        return (action?.performAction(triggerTime, configuredContext) ?: Completable.error(IllegalStateException("Not proper action instance is generated.")))
                .doOnComplete {
                    configuredContext.applicationContext.runOnUiThread {
                        LocalBroadcastManager.getInstance(configuredContext.applicationContext).sendBroadcastSync(Intent(OTApp.BROADCAST_ACTION_TRIGGER_FIRED)
                                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                                .putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
                        )
                    }
                }
    }
}