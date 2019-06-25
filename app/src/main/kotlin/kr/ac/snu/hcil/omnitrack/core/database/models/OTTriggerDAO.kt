package kr.ac.snu.hcil.omnitrack.core.database.models

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmQuery
import io.realm.RealmResults
import io.realm.annotations.Ignore
import io.realm.annotations.Index
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import kr.ac.snu.hcil.android.common.containers.Nullable
import kr.ac.snu.hcil.android.common.view.IReadonlyObjectId
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.calculation.expression.ExpressionEvaluator
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerMeasureEntry
import kr.ac.snu.hcil.omnitrack.core.flags.CreationFlagsHelper
import kr.ac.snu.hcil.omnitrack.core.flags.F
import kr.ac.snu.hcil.omnitrack.core.flags.LockFlagLevel
import kr.ac.snu.hcil.omnitrack.core.flags.LockedPropertiesHelper
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTBackgroundLoggingTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTTriggerAction
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.ATriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTDataDrivenTriggerCondition
import kr.ac.snu.hcil.omnitrack.core.triggers.conditions.OTTimeTriggerCondition
import org.jetbrains.anko.runOnUiThread

/**
 * Created by Young-Ho on 10/9/2017.
 */
@Suppress("PropertyName")
open class OTTriggerDAO : RealmObject() {

    data class SimpleTriggerInfo(override val _id: String, val conditionType: Byte, val condition: ATriggerCondition?, val actionType: Byte, val action: OTTriggerAction?, val trackers: Array<OTTrackerDAO.SimpleTrackerInfo>?) : IReadonlyObjectId

    enum class TriggerInvalidReason { TRACKER_NOT_ATTACHED, CONDITION_INVALID }
    class TriggerConfigInvalidException(vararg _causes: TriggerInvalidReason) : Exception() {
        val causes: Array<out TriggerInvalidReason> = _causes

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
    var _id: String? = null
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
                    CONDITION_TYPE_TIME -> serializedCondition?.let { OTTimeTriggerCondition.typeAdapter.fromJson(it) }
                            ?: OTTimeTriggerCondition()
                    CONDITION_TYPE_DATA -> serializedCondition?.let { OTApp.applicationComponent.dataDrivenConditionTypeAdapter().fromJson(it) }
                            ?: OTDataDrivenTriggerCondition()
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
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction()
                        ACTION_TYPE_REMIND -> OTReminderAction()
                        else -> null
                    }
                } else {
                    when (actionType) {
                        ACTION_TYPE_LOG -> OTBackgroundLoggingTriggerAction.typeAdapter.fromJson(serializedAction)
                        ACTION_TYPE_REMIND -> OTReminderAction.typeAdapter.fromJson(serializedAction)
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

    val liveTrackerIds: Array<String>
        get() {
            return if (this.isManaged) {
                liveTrackersQuery.findAll()
            } else {
                trackers.filter { !it.removed }
            }.map { it._id!! }.toTypedArray()
        }

    var synchronizedAt: Long? = null

    @Index
    var removed: Boolean = false
    var userUpdatedAt: Long = System.currentTimeMillis()
    var userCreatedAt: Long = System.currentTimeMillis()

    //this field is only used for data-driven condition.
    @LinkingObjects("triggers")
    val measureEntries: RealmResults<OTTriggerMeasureEntry>? = null

    var serializedCreationFlags: String = "{}"

    @Index
    var experimentIdInFlags: String? = null

    @Ignore
    private var _parsedLockedPropertyInfo: JsonObject? = null

    fun getParsedLockedPropertyInfo(): JsonObject {
        if (_parsedLockedPropertyInfo == null) {
            _parsedLockedPropertyInfo = LockedPropertiesHelper.parseFlags(serializedLockedPropertyInfo)
        }
        return _parsedLockedPropertyInfo!!
    }

    private val lockFlagLevel: String get(){
       return if(actionType == ACTION_TYPE_REMIND) LockFlagLevel.Reminder else LockFlagLevel.Trigger
    }

    fun isEditingAllowed(): Boolean {
        return LockedPropertiesHelper.flag(lockFlagLevel, F.Modify, getParsedLockedPropertyInfo())
    }

    fun isRemovalAllowed(): Boolean {
        return LockedPropertiesHelper.flag(lockFlagLevel, F.Delete, getParsedLockedPropertyInfo())
    }

    fun isSwitchAllowed(): Boolean {
        return LockedPropertiesHelper.flag(lockFlagLevel, F.ToggleSwitch, getParsedLockedPropertyInfo())
    }

    fun invalidateConditionCache() {
        _condition = null
    }

    fun initializeUserCreated() {

        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.DEFAULT_EXPERIMENT_ID != null) {
            experimentIdInFlags = BuildConfig.DEFAULT_EXPERIMENT_ID
            serializedCreationFlags = CreationFlagsHelper.Builder(serializedCreationFlags)
                    .setInjected(false)
                    .setExperiment(BuildConfig.DEFAULT_EXPERIMENT_ID)
                    .build()
        }

        val flags = LockedPropertiesHelper.generateDefaultFlags(lockFlagLevel, true)
        serializedLockedPropertyInfo = flags.toString()
        _parsedLockedPropertyInfo = flags
    }

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

    fun validateScriptIfExist(context: Context): Boolean {
        if (checkScript) {
            if (additionalScript == null || additionalScript?.contentEquals("TRUE") == true) {
                return true
            } else {
                //evaluate
                val evaluator = ExpressionEvaluator(additionalScript!!, *(context.applicationContext as OTAndroidApp).applicationComponent.getSupportedScriptFunctions())
                return evaluator.evalBoolean()
            }
        } else {
            return true
        }
    }

    /**
     * Check whether the trigger is valid to be turned on the system.
     *
     * @return the trigger is invalid due to the returned exception.
     */
    fun isValidToTurnOn(context: Context): Single<Nullable<Array<TriggerInvalidReason>>> {
        return (condition?.isConfigurationValid(context)?.map { (valid, _) -> valid }
                ?: Single.just(false))
                .map { isConditionValid ->
                    val containsTracker = liveTrackerCount > 0
                    val invalids = HashSet<TriggerInvalidReason>()
                    if (!containsTracker) {
                        invalids.add(TriggerInvalidReason.TRACKER_NOT_ATTACHED)
                    }
                    if (!isConditionValid) {
                        invalids.add(TriggerInvalidReason.CONDITION_INVALID)
                    }

                    return@map if (invalids.count() > 0)
                        Nullable(invalids.toTypedArray())
                    else Nullable()
                }
    }

    fun getPerformFireCompletable(triggerTime: Long, metadata: JsonObject, context: Context): Completable {
        val triggerId = _id!!

        val unManagedDAO = if (this.isManaged) this.realm.copyFromRealm(this) else this
        return (action?.performAction(this, triggerTime, metadata, context)
                ?: Completable.error(IllegalStateException("Not proper action instance is generated.")))
                .doOnComplete {
                    context.runOnUiThread {
                        LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcastSync(Intent(OTApp.BROADCAST_ACTION_TRIGGER_FIRED)
                                .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                                .putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
                        )
                        (context.applicationContext as OTAndroidApp).applicationComponent.getEventLogger()
                                .logTriggerFireEvent(triggerId, System.currentTimeMillis(), unManagedDAO) { content -> content.add("idealTime", triggerTime.toJson()) }

                    }
                }
    }

    fun getSimpleInfo(populateTracker: Boolean = false): SimpleTriggerInfo {
        return SimpleTriggerInfo(_id!!, conditionType, condition, actionType, action,
                if (populateTracker) {
                    if (isManaged) {
                        if (liveTrackerCount > 0) {
                            liveTrackersQuery.findAll().map { it.getSimpleInfo() }.toTypedArray()
                        } else emptyArray()
                    } else {
                        trackers.asSequence().filter { !it.removed }.map { it.getSimpleInfo() }.toList().toTypedArray()
                    }
                } else null
        )
    }
}