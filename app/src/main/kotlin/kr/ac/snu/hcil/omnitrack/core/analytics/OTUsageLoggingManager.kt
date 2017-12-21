package kr.ac.snu.hcil.omnitrack.core.analytics

import android.os.Looper
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import dagger.Lazy
import dagger.internal.Factory
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.ItemLoggingSource
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_ATTRIBUTE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_ITEM
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_SERVICE
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_TRACKER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_CHANGE_TRIGGER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_SESSION
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_TRACKER_DATA_EXPORT
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.NAME_TRACKER_REORDER
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_ADD
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_EDIT
import kr.ac.snu.hcil.omnitrack.core.analytics.IEventLogger.Companion.SUB_REMOVE
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.database.local.models.helpermodels.UsageLog
import kr.ac.snu.hcil.omnitrack.core.di.UsageLogger
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 11. 28..
 */
class OTUsageLoggingManager(app: OTApp) : IEventLogger {

    @field:[Inject UsageLogger]
    lateinit var realmFactory: Factory<Realm>

    @Inject
    lateinit var authManager: OTAuthManager

    @Inject
    lateinit var jobDispatcher: Lazy<FirebaseJobDispatcher>

    @field:[Inject UsageLogger]
    lateinit var uploadJob: Provider<Job>

    init {
        app.applicationComponent.inject(this)
    }

    private var logIdGenerator = ConcurrentUniqueLongGenerator()

    override fun logEvent(name: String, sub: String?, content: JsonObject?, timestamp: Long) {
        val realm = realmFactory.get()

        val transaction = { realm: Realm ->
            val newLog = realm.createObject(UsageLog::class.java, logIdGenerator.getNewUniqueLong())
            newLog.name = name
            newLog.sub = sub
            newLog.deviceId = OTApp.instance.deviceId
            newLog.userId = authManager.userId
            newLog.timestamp = timestamp
            newLog.contentJson = content?.toString() ?: "null"
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            realm.executeTransactionAsync(transaction, {
                jobDispatcher.get().mustSchedule(uploadJob.get())
                realm.close()
            }, { ex ->
                println("UsageLog save transaction was failed:")
                ex.printStackTrace()
            })
        } else {
            realm.executeTransaction(transaction)
            jobDispatcher.get().mustSchedule(uploadJob.get())
            realm.close()
        }
    }


    override fun logAttributeChangeEvent(sub: String?, attributeLocalId: String, trackerId: String?, inject: ((JsonObject) -> Unit)?) {
        val content = jsonObject(
                "localId" to attributeLocalId,
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

}