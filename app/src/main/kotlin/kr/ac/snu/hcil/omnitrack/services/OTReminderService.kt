package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.helpermodels.OTTriggerReminderEntry
import kr.ac.snu.hcil.omnitrack.core.di.configured.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.Default
import kr.ac.snu.hcil.omnitrack.core.di.global.ReminderNotification
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.ui.pages.items.ItemDetailActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.settings.SettingsActivity
import kr.ac.snu.hcil.omnitrack.utils.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableWakefulService
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.runOnUiThread
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 11. 13..
 */
class OTReminderService : ConfigurableWakefulService(TAG) {

    companion object {
        const val TAG = "OTReminderService"
        const val ACTION_PREFIX = "${BuildConfig.APPLICATION_ID}.${TAG}.action"
        const val ACTION_REMIND = "${ACTION_PREFIX}.remind"
        const val ACTION_ON_USER_ACCESS = "${ACTION_PREFIX}.user_accessed"
        const val ACTION_ON_USER_DISMISS = "${ACTION_PREFIX}.dismissed"
        const val ACTION_ON_USER_LOGGED = "${ACTION_PREFIX}.user_logged"

        const val ACTION_ON_SYSTEM_REBOOTED = "${ACTION_PREFIX}.system_rebooted"


        const val INTENT_EXTRA_ENTRY_ID = "entryId"
        const val INTENT_EXTRA_LOGGED_AT = "loggedAt"

        private fun makeBaseIntent(context: Context, configId: String, action: String): Intent {
            return Intent(context, OTReminderService::class.java)
                    .setAction(action)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
        }

        fun makeRemindIntent(context: Context, configId: String, triggerId: String, triggerTime: Long): Intent {
            return makeBaseIntent(context, configId, ACTION_REMIND)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
        }

        fun makeReminderAccessedIntent(context: Context, configId: String, triggerId: String, trackerId: String, entryId: Long, triggerTime: Long): Intent {
            return makeBaseIntent(context, configId, ACTION_ON_USER_ACCESS)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
                    .putExtra(INTENT_EXTRA_ENTRY_ID, entryId)
        }

        fun makeReminderDismissedIntent(context: Context, configId: String, triggerId: String, entryId: Long): Intent {
            return makeBaseIntent(context, configId, ACTION_ON_USER_DISMISS)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_ENTRY_ID, entryId)
        }

        fun makeUserLoggedIntent(context: Context, configId: String, trackerId: String, loggedAt: Long): Intent {
            return makeBaseIntent(context, configId, ACTION_ON_USER_LOGGED)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(INTENT_EXTRA_LOGGED_AT, loggedAt)
        }

        fun makeSystemRebootedIntent(context: Context, configId: String): Intent {
            return makeBaseIntent(context, configId, ACTION_ON_SYSTEM_REBOOTED)
        }

        val dateFormat: DateFormat by lazy {
            SimpleDateFormat("hh:mm:ss")
        }

        internal fun handleEntrySyncImpl(entryId: Long, realm: Realm, handler: (OTTriggerReminderEntry) -> Unit) {
            val entry = realm.where(OTTriggerReminderEntry::class.java)
                    .equalTo("id", entryId).findFirst()
            if (entry != null) {
                handler.invoke(entry)
            }
        }

        internal fun dismissSyncImpl(entryId: Long, realm: Realm, context: Context) {
            handleEntrySyncImpl(entryId, realm) { entry ->
                if (Build.VERSION.SDK_INT < 26) {
                    WorkManager.getInstance().cancelAllWorkByTag(entry.id.toString())
                }
                entry.realm.executeTransaction { realm ->
                    entry.dismissed = true
                }
                val notificationId = entry.systemIntrinsicId //to avoid

                context.runOnUiThread {
                    notificationManager.cancel(TAG, notificationId)
                }
            }
        }
    }

    inner class ConfiguredTask(startId: Int, configuredContext: ConfiguredContext) : AConfiguredTask(startId, configuredContext) {


        private val entryIdGenerator = ConcurrentUniqueLongGenerator()


        @field:[Inject ReminderNotification]
        lateinit var notificationIdSeed: AtomicInteger

        @field:[Inject Default]
        protected lateinit var pref: SharedPreferences

        @field:[Inject Backend]
        protected lateinit var realmProvider: Factory<Realm>

        @Inject
        protected lateinit var dbManager: BackendDbManager

        private val subscriptions = CompositeDisposable()

        init {
            configuredContext.configuredAppComponent.inject(this)
        }

        override fun dispose() {
            subscriptions.clear()
        }

        override fun onStartCommand(intent: Intent, flags: Int): Int {
            OTApp.logger.writeSystemLog("Start OTReminderService with command ${intent.action}", TAG)
            val completable = when (intent.action) {
                ACTION_REMIND -> remind(
                        startId,
                        intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                        intent.getLongExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())
                )
                ACTION_ON_USER_ACCESS -> {
                    onUserAccessed(startId,
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER),
                            intent.getLongExtra(INTENT_EXTRA_ENTRY_ID, 0),
                            intent.getLongExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())
                    )
                }
                ACTION_ON_USER_DISMISS -> {
                    onUserDismissed(startId,
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                            intent.getLongExtra(INTENT_EXTRA_ENTRY_ID, 0)
                    )
                }
                ACTION_ON_USER_LOGGED -> {
                    onUserLogged(startId, intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER),
                            intent.getLongExtra(INTENT_EXTRA_LOGGED_AT, System.currentTimeMillis())
                    )
                }
                ACTION_ON_SYSTEM_REBOOTED -> {
                    onSystemRebooted(startId)
                }
                else -> Completable.complete()
            }

            subscriptions.add(
                    completable.observeOn(AndroidSchedulers.mainThread())
                            .doOnTerminate {
                                finishSelf()
                            }
                            .subscribe({
                                OTApp.logger.writeSystemLog("Successfully handled reminder service action.", TAG)
                            }, { error ->
                                OTApp.logger.writeSystemLog("Error while handling the reminder service action - ${intent.action},\n${Log.getStackTraceString(error)}", TAG)
                                error.printStackTrace()
                            })
            )

            return START_NOT_STICKY
        }

        private fun onUserLogged(startId: Int, trackerId: String, loggedAt: Long): Completable {
            return Completable.defer {
                val realm = realmProvider.get()

                val entries = realm.where(OTTriggerReminderEntry::class.java)
                        .equalTo("dismissed", false)
                        .equalTo("trackerId", trackerId)
                        .findAll()

                entries.forEach { entry ->
                    when (entry.level) {
                        OTReminderAction.NotificationLevel.Noti -> {
                            notificationManager.cancel(TAG, entry.systemIntrinsicId)
                        }
                        OTReminderAction.NotificationLevel.Popup -> {

                        }
                        OTReminderAction.NotificationLevel.Impose -> {

                        }
                    }
                }

                realm.executeTransaction {
                    entries.deleteAllFromRealm()
                }

                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun onUserAccessed(startId: Int, triggerId: String, trackerId: String, entryId: Long, triggerTime: Long): Completable {
            return handleEntry(entryId) { entry ->
                entry.realm.executeTransaction {
                    entry.accessedAt = System.currentTimeMillis()
                }
            }.andThen(Completable.defer {
                val realm = realmProvider.get()
                val trigger = dbManager.getTriggerQueryWithId(triggerId, realm).findFirst()
                if (trigger != null) {
                    trigger.liveTrackersQuery.equalTo(BackendDbManager.FIELD_OBJECT_ID, trackerId).findFirst()?.let {
                        ItemDetailActivity.makeReminderOpenIntent(it.objectId!!, triggerTime, this@OTReminderService)
                    }?.let {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        this@OTReminderService.startActivity(it)
                    }
                }
                realm.close()
                return@defer Completable.complete()
            })
        }

        private fun onUserDismissed(startId: Int, triggerId: String, entryId: Long): Completable {
            return dismiss(entryId)
        }

        private fun onSystemRebooted(startId: Int): Completable {
            return Completable.defer {
                val realm = realmProvider.get()

                //handle all omitted reminders.

                val pendingEntries = realm.where(OTTriggerReminderEntry::class.java)
                        .equalTo("dismissed", false)
                        .sort("autoExpireAt", Sort.DESCENDING)
                        .findAll()
                val entriesByTracker = pendingEntries.groupBy { entry -> entry.trackerId!! }
                realm.executeTransactionIfNotIn { realm ->
                    entriesByTracker.forEach { trackerId, entries ->
                        if (entries.isNotEmpty()) {
                            if (entries.first().autoExpireAt >= System.currentTimeMillis() + 5000) {
                                val tracker = dbManager.getTrackerQueryWithId(trackerId, realm).findFirst()
                                if (tracker != null) {
                                    val entry = realm.createObject(OTTriggerReminderEntry::class.java, entryIdGenerator.getNewUniqueLong(System.currentTimeMillis()))
                                    entry.level = OTReminderAction.NotificationLevel.Noti
                                    entry.triggerId = entries.first().triggerId
                                    entry.trackerId = trackerId
                                    entry.autoExpireAt = entries.first().autoExpireAt

                                    if (entries.first().autoExpireAt != Long.MAX_VALUE) {
                                        entry.timeoutDuration = (entries.first().autoExpireAt - System.currentTimeMillis()).toInt()
                                    }

                                    entry.intrinsicTriggerTime = entries.first().intrinsicTriggerTime
                                    entry.notifiedAt = System.currentTimeMillis()
                                    val notificationId = notificationIdSeed.incrementAndGet()
                                    entry.systemIntrinsicId = notificationId
                                    val notiBuilder = makeReminderNotificationBuilderBase(notificationId, entry.triggerId!!, trackerId, tracker.name, entry.id, entry.intrinsicTriggerTime, entries.first().autoExpireAt, entry.timeoutDuration?.toLong(), entries.size)
                                    notiBuilder.setContentTitle(OTApp.getString(R.string.msg_reminder_omitted))

                                    if (entry.autoExpireAt < Long.MAX_VALUE) reserveAutoExpiry(entry)

                                    runOnUiThread {
                                        notificationManager.notify(TAG, notificationId, notiBuilder.build())
                                    }
                                }

                            }
                        }
                    }
                }

                println("Before reboot, ${pendingEntries.size} reminders from ${entriesByTracker.keys.size} trackers were omitted.")

                realm.executeTransactionIfNotIn {
                    pendingEntries.deleteAllFromRealm()
                }

                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun remind(startId: Int, triggerId: String, triggerTime: Long): Completable {
            return Completable.defer {
                val realm = realmProvider.get()

                val trigger = dbManager.getTriggerQueryWithId(triggerId, realm).findFirst()
                if (trigger != null
                        && trigger.actionType == OTTriggerDAO.ACTION_TYPE_REMIND
                        && trigger.liveTrackerCount > 0) {
                    val action = trigger.action as OTReminderAction

                    when (action.notificationLevelForSystem) {
                        OTReminderAction.NotificationLevel.Noti -> {
                            trigger.liveTrackersQuery.findAll().iterator().forEach { tracker ->

                                val pendingEntries = realm.where(OTTriggerReminderEntry::class.java)
                                        .equalTo("dismissed", false)
                                        .equalTo("trackerId", tracker.objectId!!)
                                        .findAll()
                                val notificationIds = pendingEntries.map { it.systemIntrinsicId }
                                runOnUiThread {
                                    notificationIds.forEach {
                                        notificationManager.cancel(TAG, it)
                                    }
                                }

                                val dismissedNotificationCount = pendingEntries.count()

                                realm.executeTransactionIfNotIn {

                                    pendingEntries.forEach { it.dismissed = true }

                                    val entry = insertNewReminderEntry(trigger, tracker.objectId!!, action, triggerTime, realm)
                                    val notificationId = notificationIdSeed.incrementAndGet()
                                    entry.systemIntrinsicId = notificationId
                                    val notiBuilder = makeReminderNotificationBuilderBase(notificationId, trigger.objectId!!, tracker.objectId!!, tracker.name, entry.id, triggerTime, entry.autoExpireAt, entry.timeoutDuration?.toLong(), dismissedNotificationCount)
                                    if (action.message?.isNotBlank() == true) {
                                        notiBuilder.setContentTitle(action.message)
                                    } else notiBuilder.setContentTitle(String.format(OTApp.getString(R.string.msg_format_reminder_noti_title), tracker.name))

                                    if (entry.autoExpireAt < Long.MAX_VALUE) reserveAutoExpiry(entry)

                                    runOnUiThread {
                                        OTApp.logger.writeSystemLog("Show reminder notification of trigger", TAG)
                                        notificationManager.notify(TAG, notificationId, notiBuilder.build())
                                    }
                                }
                            }
                        }
                        OTReminderAction.NotificationLevel.Popup -> {

                        }
                        OTReminderAction.NotificationLevel.Impose -> {

                        }
                    }
                }
                realm.close()
                return@defer Completable.complete()
            }
                    .subscribeOn(Schedulers.io())

        }

        private fun handleEntry(entryId: Long, handler: (OTTriggerReminderEntry) -> Unit): Completable {
            return Completable.defer {
                val realm = realmProvider.get()
                handleEntrySyncImpl(entryId, realm, handler)
                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun dismiss(entryId: Long): Completable {
            return Completable.defer {
                val realm = realmProvider.get()
                dismissSyncImpl(entryId, realm, this@OTReminderService)
                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun insertNewReminderEntry(trigger: OTTriggerDAO, trackerId: String, action: OTReminderAction, triggerTime: Long, realm: Realm): OTTriggerReminderEntry {
            val entry = realm.createObject(OTTriggerReminderEntry::class.java, entryIdGenerator.getNewUniqueLong(System.currentTimeMillis()))
            entry.level = action.notificationLevelForSystem
            entry.triggerId = trigger.objectId
            entry.trackerId = trackerId
            entry.intrinsicTriggerTime = triggerTime
            entry.autoExpireAt = (trigger.action as? OTReminderAction)?.expirySeconds?.let { (it * 1000) + System.currentTimeMillis() } ?: Long.MAX_VALUE
            entry.timeoutDuration = (trigger.action as? OTReminderAction)?.expirySeconds?.let { it * 1000 }
            entry.notifiedAt = System.currentTimeMillis()

            return entry
        }

        /**
         * reserve auto expiry. Don't need it from android O.
         */
        private fun reserveAutoExpiry(entry: OTTriggerReminderEntry) {
            if (Build.VERSION.SDK_INT >= 26) {
                return
            }

            if (entry.timeoutDuration != null) {
                WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<ReminderDismissWorker>()
                        .setInitialDelay(entry.timeoutDuration!!.toLong(), TimeUnit.MILLISECONDS)
                        .setInputData(Data.Builder()
                                .putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, entry.triggerId)
                                .putLong(INTENT_EXTRA_ENTRY_ID, entry.id)
                                .putString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configuredContext.configuration.id)
                                .build())
                        .addTag(entry.id.toString())
                        .build())
            }
        }


        private fun makeReminderNotificationBuilderBase(notiId: Int, triggerId: String, trackerId: String, trackerName: String, entryId: Long, reminderTime: Long, expireAt: Long, durationMs: Long?, dismissedCount: Int): NotificationCompat.Builder {

            println("reminderTime: $reminderTime, expireAt: $expireAt")
            /*
            val stackBuilder = TaskStackBuilder.create(this)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(ItemDetailActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(ItemDetailActivity.makeIntent(trackerId, reminderTime, this))
            val resultPendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT)
    */
            val accessPendingIntent = PendingIntent.getService(this@OTReminderService, notiId, makeReminderAccessedIntent(this@OTReminderService, configuredContext.configuration.id, triggerId, trackerId, entryId, reminderTime), PendingIntent.FLAG_UPDATE_CURRENT)
            val dismissIntent = PendingIntent.getService(this@OTReminderService, notiId,
                    makeReminderDismissedIntent(this@OTReminderService, configuredContext.configuration.id, triggerId, entryId),
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val ringtone = pref.getString(SettingsActivity.PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString())
            val lightColor = pref.getInt(SettingsActivity.PREF_REMINDER_LIGHT_COLOR, ContextCompat.getColor(this@OTReminderService, R.color.colorPrimary))
            val contentTextBase = String.format(OTApp.getString(R.string.msg_noti_tap_for_tracking_format), trackerName)

            val contentText: String = if (expireAt < Long.MAX_VALUE) {
                contentTextBase + " (${String.format(OTApp.getString(R.string.msg_noti_reminder_until), dateFormat.format(Date(expireAt)))})"
            } else contentTextBase

            return OTTrackingNotificationFactory.makeBaseBuilder(this@OTReminderService, reminderTime, OTNotificationManager.CHANNEL_ID_IMPORTANT)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSound(Uri.parse(
                            ringtone
                    ))
                    .setLights(lightColor, 1000, 500)
                    //.setContentText(if(dismissedCount==0) contentTextBase else "$contentTextBase (${resources.getQuantityString(R.plurals.msg_reminder_omitted, dismissedCount)})")
                    .setContentText(contentText)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .apply { if (durationMs != null) this.setTimeoutAfter(durationMs) }
                    .addAction(NotificationCompat.Action.Builder(0, OTApp.getString(R.string.msg_reminder_open_input), accessPendingIntent).build())
                    .addAction(NotificationCompat.Action.Builder(0, OTApp.getString(R.string.msg_reminder_skip), dismissIntent).build())
                    .setDeleteIntent(dismissIntent)
                    .setContentIntent(accessPendingIntent)
            //.setFullScreenIntent(accessPendingIntent, true)

        }

    }

    override fun onInject(app: OTApp) {
        app.applicationComponent.inject(this)
    }

    override fun makeConfiguredTask(startId: Int, configuredContext: ConfiguredContext): AConfiguredTask {
        return ConfiguredTask(startId, configuredContext)
    }

    class ReminderDismissWorker : Worker() {

        @field:[Inject Backend]
        protected lateinit var realmProvider: Factory<Realm>

        override fun doWork(): WorkerResult {
            return try {
                val triggerId = inputData.getString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, "")
                val entryId = inputData.getLong(INTENT_EXTRA_ENTRY_ID, 0)
                val configId = inputData.getString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, "")

                val configuredContext = (applicationContext as OTApp).applicationComponent.configurationController().getConfiguredContextOf(configId)
                configuredContext?.configuredAppComponent?.inject(this)
                if (configuredContext != null) {
                    //this.applicationContext.startService(OTReminderService.makeReminderDismissedIntent(this.applicationContext, configId, triggerId, entryId))
                    val realm = realmProvider.get()
                    OTReminderService.dismissSyncImpl(entryId, realm, applicationContext)
                    realm.close()
                    OTApp.logger.writeSystemLog("Successfully dismissed reminder by Worker. entryId: $entryId", "ReminderDismissWorker")
                    WorkerResult.SUCCESS
                } else WorkerResult.FAILURE
            } catch (ex: Exception) {
                OTApp.logger.writeSystemLog("ReminderDismissWorker doWork error: \n${Log.getStackTraceString(ex)}", "ReminderDismissWorker")
                WorkerResult.FAILURE
            }
        }
    }
}