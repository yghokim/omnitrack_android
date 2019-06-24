package kr.ac.snu.hcil.omnitrack.core.analytics

import android.content.Context
import android.os.Looper
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.android.common.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.android.common.versionCode
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_ATTRIBUTE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_ITEM
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_SERVICE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_TRACKER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_TRIGGER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_DEVICE_STATUS_CHANGE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_EXCEPTION
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_SESSION
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_TRACKER_DATA_EXPORT
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_TRACKER_REORDER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_ADD
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_EDIT
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_REMOVE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.TRIGGER_FIRED
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.global.UsageLogger
import kr.ac.snu.hcil.omnitrack.core.workers.OTUsageLogUploadWorker
import org.jetbrains.anko.getStackTraceString
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 28..
 */
class OTUsageLoggingManager(val app: OTAndroidApp, val context: Context) : IEventLogger {

    @field:[Inject UsageLogger]
    lateinit var realmFactory: Factory<Realm>

    @field:[Inject UsageLogger]
    lateinit var uploadRequestProvider: Provider<OneTimeWorkRequest>

    init {
        app.applicationComponent.inject(this)
    }

    private var logIdGenerator = ConcurrentUniqueLongGenerator()

    override fun logEvent(name: String, sub: String?, content: JsonObject?, timestamp: Long) {
        println("New usage log event: $name, $sub, $content, at $timestamp")
        val realm = realmFactory.get()

        val transaction = { realm: Realm ->
            val newLog = realm.createObject(UsageLog::class.java, logIdGenerator.getNewUniqueLong())
            newLog.name = name
            newLog.sub = sub
            newLog.deviceId = app.applicationComponent.application().deviceId
            newLog.userId = app.applicationComponent.getAuthManager().userId
            newLog.timestamp = timestamp
            newLog.contentJson = content?.toString() ?: "null"
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            realm.executeTransactionAsync(transaction, {
                WorkManager.getInstance().enqueueUniqueWork(OTUsageLogUploadWorker.TAG, ExistingWorkPolicy.REPLACE, uploadRequestProvider.get())
                realm.close()
            }, { ex ->
                println("UsageLog save transaction was failed:")
                ex.printStackTrace()
                OTApp.logger.writeSystemLog(ex.getStackTraceString(), "UsageLogger")
            })
        } else {
            realm.executeTransaction(transaction)
            WorkManager.getInstance().enqueueUniqueWork(OTUsageLogUploadWorker.TAG, ExistingWorkPolicy.REPLACE, uploadRequestProvider.get())
            realm.close()
        }
    }


    override fun logTriggerFireEvent(triggerId: String, triggerFiredTime: Long, trigger: OTTriggerDAO, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "triggerId" to triggerId,
                "conditionType" to trigger.conditionType,
                "actionType" to trigger.actionType
        )

        trigger.condition?.writeEventLogContent(content)
        trigger.action?.writeEventLogContent(trigger, content)

        inject?.invoke(content)

        logEvent(TRIGGER_FIRED, null, content, triggerFiredTime)
    }

    override fun logAttributeChangeEvent(sub: String?, fieldLocalId: String, trackerId: String?, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "localId" to fieldLocalId,
                "tracker" to (trackerId ?: "unmanaged")
        )

        inject?.invoke(content)

        logEvent(NAME_CHANGE_ATTRIBUTE, sub, content)
    }

    override fun logTrackerChangeEvent(sub: String?, trackerId: String?, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "tracker" to (trackerId ?: "unmanaged")
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_TRACKER, sub, content)
    }

    override fun logTrackerOnShortcutChangeEvent(trackerId: String?, isOnShortcut: Boolean) {
        val content = jsonObject(
                "tracker" to (trackerId ?: "unmanaged"),
                "switch" to isOnShortcut
        )
        logEvent(NAME_CHANGE_TRACKER, "changeBookmarked", content)
    }

    override fun logTriggerChangeEvent(sub: String?, triggerId: String?, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "trigger" to (triggerId ?: "unmanaged")
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_TRIGGER, sub, content)
    }

    override fun logSession(sessionName: String, sessionType: String, elapsed: Long, finishedAt: Long, from: String?, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "session" to sessionName,
                "elapsed" to elapsed,
                "finishedAt" to finishedAt
        )

        if (from != null) {
            content["from"] = from
        }

        inject?.invoke(content)

        logEvent(NAME_SESSION, sessionType, content)
    }

    override fun logExport(trackerId: String?) {
        logEvent(NAME_TRACKER_DATA_EXPORT, trackerId, null)
    }

    override fun logTrackerReorderEvent() {
        logEvent(NAME_TRACKER_REORDER, null, null)
    }

    override fun logItemEditEvent(itemId: String, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "item" to itemId
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_ITEM, SUB_EDIT, content)
    }

    override fun logItemAddedEvent(itemId: String, source: ItemLoggingSource, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "item" to itemId,
                "source" to source.name
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_ITEM, SUB_ADD, content)
    }

    override fun logItemRemovedEvent(itemId: String, removedFrom: String, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "item" to itemId,
                "removedFrom" to removedFrom
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_ITEM, SUB_REMOVE, content)
    }

    override fun logServiceActivationChangeEvent(serviceCode: String, isActivated: Boolean, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "serviceCode" to serviceCode,
                "isActivated" to isActivated
        )
        inject?.invoke(content)

        logEvent(NAME_CHANGE_SERVICE, "ACTIVATION", content)
    }


    override fun logDeviceStatusChangeEvent(sub: String, batteryPercentage: Float, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                IEventLogger.CONTENT_KEY_BATTERY_PERCENTAGE to batteryPercentage
        )
        inject?.invoke(content)

        logEvent(NAME_DEVICE_STATUS_CHANGE, sub, content)
    }

    override fun logExceptionEvent(sub: String, throwable: Throwable, thread: Thread?, inject: ((JsonObject) -> Unit)?) {
        val stackTraceStringBuilder = StringBuilder(throwable.getStackTraceString())
        var cause: Throwable = throwable
        while (cause.cause != null) {
            cause = cause.cause!!
            stackTraceStringBuilder.append("\ncausedBy:\n")
            stackTraceStringBuilder.append(cause.getStackTraceString())
        }

        val content: JsonObject = jsonObject(
                "versionCode" to context.applicationContext.versionCode(),
                "experiment" to BuildConfig.DEFAULT_EXPERIMENT_ID,
                "packageName" to app.getPackageName(),
                "message" to throwable.message,
                "thread" to thread?.name,
                "stacktrace" to stackTraceStringBuilder.toString()
        )
        inject?.invoke(content)
        logEvent(NAME_EXCEPTION, sub, content)
    }

}