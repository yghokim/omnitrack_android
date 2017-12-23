package kr.ac.snu.hcil.omnitrack.services

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
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

        const val INTENT_EXTRA_ENTRY_ID = "entryId"
        const val INTENT_EXTRA_LOGGED_AT = "loggedAt"

        fun makeRemindIntent(context: Context, configId: String, triggerId: String, triggerTime: Long): Intent {
            return Intent(context, OTReminderService::class.java)
                    .setAction(ACTION_REMIND)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
        }

        fun makeReminderAccessedIntent(context: Context, configId: String, triggerId: String, trackerId: String, entryId: Long): Intent {
            return Intent(context, OTReminderService::class.java)
                    .setAction(ACTION_ON_USER_ACCESS)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_ENTRY_ID, entryId)
        }

        fun makeReminderDismissedIntent(context: Context, configId: String, triggerId: String, entryId: Long): Intent {
            return Intent(context, OTReminderService::class.java)
                    .setAction(ACTION_ON_USER_DISMISS)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                    .putExtra(INTENT_EXTRA_ENTRY_ID, entryId)
        }

        fun makeUserLoggedIntent(context: Context, configId: String, trackerId: String, loggedAt: Long): Intent {
            return Intent(context, OTReminderService::class.java)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
                    .setAction(ACTION_ON_USER_LOGGED)
                    .putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
                    .putExtra(INTENT_EXTRA_LOGGED_AT, loggedAt)
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
                            intent.getLongExtra(INTENT_EXTRA_ENTRY_ID, 0)
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
                else -> Completable.complete()
            }

            subscriptions.add(
                    completable.observeOn(AndroidSchedulers.mainThread())
                            .doOnTerminate {
                                finishSelf()
                            }
                            .subscribe {
                                OTApp.logger.writeSystemLog("Successfully handled reminder service action.", TAG)
                            }
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

                realm.executeTransaction {
                    entries.forEach { entry ->
                        entry.loggedAt = loggedAt
                        entry.dismissed = true
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
                }

                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun onUserAccessed(startId: Int, triggerId: String, trackerId: String, entryId: Long): Completable {
            return handleEntry(entryId) { entry ->
                entry.realm.executeTransaction {
                    entry.accessedAt = System.currentTimeMillis()
                }
            }.andThen(Completable.defer {
                val realm = realmProvider.get()
                val trigger = dbManager.getTriggerQueryWithId(triggerId, realm).findFirst()
                if (trigger != null) {
                    trigger.liveTrackersQuery.equalTo(BackendDbManager.FIELD_OBJECT_ID, trackerId).findFirst()?.let {
                        ItemDetailActivity.makeNewItemPageIntent(it.objectId!!, this@OTReminderService)
                    }?.let {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
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
                                realm.executeTransactionIfNotIn {
                                    val entry = insertNewReminderEntry(trigger, tracker.objectId!!, action, triggerTime, realm)
                                    val notificationId = notificationIdSeed.incrementAndGet()
                                    entry.systemIntrinsicId = notificationId
                                    val notiBuilder = makeReminderNotificationBuilderBase(notificationId, trigger.objectId!!, tracker.objectId!!, tracker.name, entry.id, triggerTime)
                                    if (action.message?.isNotBlank() == true) {
                                        notiBuilder.setContentTitle(action.message)
                                    } else notiBuilder.setContentTitle(String.format(OTApp.getString(R.string.msg_format_reminder_noti_title), tracker.name))


                                    runOnUiThread { notificationManager.notify(TAG, notificationId, notiBuilder.build()) }
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

                val entry = realm.where(OTTriggerReminderEntry::class.java)
                        .equalTo("id", entryId).findFirst()
                if (entry != null) {
                    handler.invoke(entry)
                }

                realm.close()
                return@defer Completable.complete()
            }.subscribeOn(Schedulers.io())
        }

        private fun dismiss(entryId: Long): Completable {
            return handleEntry(entryId) { entry ->
                entry.realm.executeTransaction { realm ->
                    entry.dismissed = true
                }
            }
        }

        private fun insertNewReminderEntry(trigger: OTTriggerDAO, trackerId: String, action: OTReminderAction, triggerTime: Long, realm: Realm): OTTriggerReminderEntry {
            val entry = realm.createObject(OTTriggerReminderEntry::class.java, entryIdGenerator.getNewUniqueLong(System.currentTimeMillis()))
            entry.level = action.notificationLevelForSystem
            entry.triggerId = trigger.objectId
            entry.trackerId = trackerId
            entry.intrinsicTriggerTime = triggerTime
            entry.notifiedAt = System.currentTimeMillis()

            return entry
        }

        private fun makeReminderNotificationBuilderBase(notiId: Int, triggerId: String, trackerId: String, trackerName: String, entryId: Long, reminderTime: Long): NotificationCompat.Builder {

            /*
            val stackBuilder = TaskStackBuilder.create(this)
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(ItemDetailActivity::class.java)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(ItemDetailActivity.makeIntent(trackerId, reminderTime, this))
            val resultPendingIntent = stackBuilder.getPendingIntent(0,
                    PendingIntent.FLAG_UPDATE_CURRENT)
    */
            val accessPendingIntent = PendingIntent.getService(this@OTReminderService, notiId, makeReminderAccessedIntent(this@OTReminderService, configuredContext.configuration.id, triggerId, trackerId, entryId), PendingIntent.FLAG_UPDATE_CURRENT)
            val dismissIntent = makeReminderDismissedIntent(this@OTReminderService, configuredContext.configuration.id, triggerId, entryId)

            val ringtone = pref.getString(SettingsActivity.PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString())
            val lightColor = pref.getInt(SettingsActivity.PREF_REMINDER_LIGHT_COLOR, ContextCompat.getColor(this@OTReminderService, R.color.colorPrimary))

            return OTTrackingNotificationFactory.makeBaseBuilder(this@OTReminderService, reminderTime, OTNotificationManager.CHANNEL_ID_IMPORTANT)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSound(Uri.parse(
                            ringtone
                    ))
                    .setLights(lightColor, 1000, 500)
                    .setContentText(String.format(OTApp.getString(R.string.msg_noti_tap_for_tracking_format), trackerName))
                    .setAutoCancel(false)
                    .setDeleteIntent(PendingIntent.getService(this@OTReminderService, notiId, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentIntent(accessPendingIntent)
                    .setFullScreenIntent(accessPendingIntent, true)


            /*
            if (OTTrackingNotificationFactory.reminderTrackerPendingCounts.containsKey(tracker.objectId)) {
                println("merge reminder - ${tracker.name}")
                //not first. merge notification
                OTTrackingNotificationFactory.reminderTrackerPendingCounts[tracker.objectId] = OTTrackingNotificationFactory.reminderTrackerPendingCounts[tracker.objectId]!! + 1

                builder.setAutoCancel(false)
                        .setContentTitle("${OTTrackingNotificationFactory.reminderTrackerPendingCounts[tracker.objectId]} ${tracker.name} Reminders")
            } else {
                println("show new reminder - ${tracker.name}")
                //first time. this is the only notification with that tracker.
                OTTrackingNotificationFactory.reminderTrackerPendingCounts[tracker.objectId] = 1

                builder.setAutoCancel(true)
                        .setContentTitle("${tracker.name} Reminder")
            }


            OTTrackingNotificationFactory.notificationService.notify(OTTrackingNotificationFactory.TAG, OTTrackingNotificationFactory.getNewReminderNotificationId(tracker), builder.build())*/
        }

    }

    override fun onInject(app: OTApp) {
        app.applicationComponent.inject(this)
    }

    override fun makeConfiguredTask(startId: Int, configuredContext: ConfiguredContext): AConfiguredTask {
        return ConfiguredTask(startId, configuredContext)
    }
}