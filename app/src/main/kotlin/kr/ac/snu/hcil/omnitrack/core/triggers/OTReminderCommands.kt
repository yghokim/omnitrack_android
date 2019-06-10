package kr.ac.snu.hcil.omnitrack.core.triggers

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import io.realm.Sort
import kr.ac.snu.hcil.android.common.ConcurrentUniqueLongGenerator
import kr.ac.snu.hcil.android.common.isInteractiveCompat
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTrackerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.OTTriggerDAO
import kr.ac.snu.hcil.omnitrack.core.database.models.helpermodels.OTTriggerReminderEntry
import kr.ac.snu.hcil.omnitrack.core.di.global.Backend
import kr.ac.snu.hcil.omnitrack.core.di.global.Default
import kr.ac.snu.hcil.omnitrack.core.di.global.ForGeneric
import kr.ac.snu.hcil.omnitrack.core.di.global.ReminderNotification
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.core.system.OTTrackingNotificationFactory
import kr.ac.snu.hcil.omnitrack.core.triggers.actions.OTReminderAction
import kr.ac.snu.hcil.omnitrack.receivers.TimeTriggerAlarmReceiver
import kr.ac.snu.hcil.omnitrack.services.OTDeviceStatusService
import kr.ac.snu.hcil.omnitrack.services.OTReminderService
import kr.ac.snu.hcil.omnitrack.ui.pages.configs.SettingsActivity
import kr.ac.snu.hcil.omnitrack.ui.pages.items.NewItemActivity
import kr.ac.snu.hcil.omnitrack.utils.executeTransactionIfNotIn
import org.jetbrains.anko.alarmManager
import org.jetbrains.anko.notificationManager
import org.jetbrains.anko.powerManager
import org.jetbrains.anko.runOnUiThread
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Provider

class OTReminderCommands(val context: Context) {

    companion object {
        const val TAG = "OTReminderCommands"

        val dateFormat: DateFormat by lazy {
            SimpleDateFormat("hh:mm:ss")
        }
    }

    private val entryIdGenerator = ConcurrentUniqueLongGenerator()

    @field:[Inject ReminderNotification]
    lateinit var notificationIdSeed: AtomicInteger

    @field:[Inject Default]
    protected lateinit var pref: SharedPreferences

    @field:[Inject Backend]
    protected lateinit var realmProvider: Factory<Realm>

    @field:[Inject ForGeneric]
    protected lateinit var gson: Provider<Gson>

    fun getNewRealm(): Realm {
        return realmProvider.get()
    }

    @Inject
    protected lateinit var dbManager: BackendDbManager

    init {
        (context.applicationContext as OTAndroidApp).applicationComponent.inject(this)
    }

    fun isReminderPromptingToTracker(trackerId: String): Boolean {
        val realm = getNewRealm()
        val count = realm.where(OTTriggerReminderEntry::class.java)
                .equalTo("dismissed", false)
                .equalTo("trackerId", trackerId)
                .count()
        realm.close()
        return count > 0
    }

    internal fun onUserLogged(startId: Int, trackerId: String, loggedAt: Long): Completable {
        return Completable.defer {
            val realm = getNewRealm()

            val entries = realm.where(OTTriggerReminderEntry::class.java)
                    .equalTo("dismissed", false)
                    .equalTo("trackerId", trackerId)
                    .findAll()

            entries.forEach { entry ->
                when (entry.level) {
                    OTReminderAction.NotificationLevel.Noti -> {
                        context.notificationManager.cancel(TAG, entry.systemIntrinsicId)
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

    internal fun onUserAccessed(startId: Int, triggerId: String, trackerId: String, entryId: Long, triggerTime: Long): Completable {
        return Completable.defer {
            val realm = realmProvider.get()
            val entry = realm.where(OTTriggerReminderEntry::class.java)
                    .equalTo("id", entryId).findFirst()
            if (entry != null) {
                val metadata = entry.serializedMetadata?.let { gson.get().fromJson(it, JsonObject::class.java) }
                        ?: JsonObject()
                metadata.addProperty("screenAccessedAt", System.currentTimeMillis())
                entry.realm.executeTransaction {
                    entry.accessedAt = System.currentTimeMillis()
                    entry.serializedMetadata = metadata.toString()
                }

                val trigger = dbManager.getTriggerQueryWithId(triggerId, realm).findFirst()
                if (trigger != null) {
                    trigger.liveTrackersQuery.equalTo(BackendDbManager.FIELD_OBJECT_ID, trackerId).findFirst()?.let {
                        NewItemActivity.makeReminderOpenIntent(it._id!!, triggerTime, metadata, context)
                    }?.let {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        context.runOnUiThread {
                            startActivity(it)

                        }
                    }
                }
            }

            realm.close()
            return@defer Completable.complete()
        }.subscribeOn(Schedulers.io())
    }

    internal fun onUserDismissed(startId: Int, triggerId: String, entryId: Long): Completable {
        return dismiss(entryId)
    }

    internal fun onSystemRebooted(): Completable {
        return Completable.defer {
            val realm = realmProvider.get()

            //handle all omitted reminders.

            handlSystemRebootSyncImpl(realm)

            realm.close()
            return@defer Completable.complete()
        }.subscribeOn(Schedulers.io())
    }

    internal fun remind(triggerId: String, triggerTime: Long, metadata: JsonObject): Completable {
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
                                    .equalTo("trackerId", tracker._id!!)
                                    .findAll()
                            val notificationIds = pendingEntries.map { it.systemIntrinsicId }
                            context.runOnUiThread {
                                notificationIds.forEach {
                                    context.notificationManager.cancel(TAG, it)
                                }
                            }

                            val dismissedNotificationCount = pendingEntries.count()

                            realm.executeTransactionIfNotIn {

                                pendingEntries.forEach { it.dismissed = true }

                                val entry = insertNewReminderEntry(trigger, tracker._id!!, action, triggerTime, metadata, realm)
                                val notiBuilder = buildNotificationFromEntry(entry, trigger, tracker)
                                if (notiBuilder != null) {
                                    notifyNotification(entry, notiBuilder.build(), true)
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

    private fun dismiss(entryId: Long): Completable {
        return Completable.defer {
            val realm = realmProvider.get()
            dismissSyncImpl(entryId, realm)
            realm.close()
            return@defer Completable.complete()
        }.subscribeOn(Schedulers.io())
    }

    private fun buildNotificationFromEntry(entry: OTTriggerReminderEntry, cachedTrigger: OTTriggerDAO?, cachedTracker: OTTrackerDAO?): NotificationCompat.Builder? {
        val tracker = cachedTracker
                ?: entry.realm.where(OTTrackerDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, entry.trackerId!!).findFirst()
        val trigger = cachedTrigger
                ?: entry.realm.where(OTTriggerDAO::class.java).equalTo(BackendDbManager.FIELD_OBJECT_ID, entry.triggerId!!).findFirst()
        if (tracker != null && trigger != null) {
            val notificationId = notificationIdSeed.incrementAndGet()
            entry.realm.executeTransactionIfNotIn {
                entry.systemIntrinsicId = notificationId
            }
            val message = (trigger.action as OTReminderAction).message?.let {
                if (it.isNotBlank() == true) it
                else null
            }
                    ?: String.format(context.getString(R.string.msg_format_reminder_noti_title), tracker.name)


            return makeReminderNotificationBuilderBase(notificationId, entry.triggerId!!, entry.trackerId!!, tracker.name, entry.id, entry.intrinsicTriggerTime, entry.autoExpireAt, entry.timeoutDuration?.toLong(), 0, message)
        } else return null
    }

    private fun notifyNotification(entry: OTTriggerReminderEntry, notification: Notification, wakeScreen: Boolean) {
        val notificationId = entry.systemIntrinsicId
        val expireAt = entry.autoExpireAt
        context.runOnUiThread {
            OTApp.logger.writeSystemLog("Show reminder notification of trigger", TAG)
            context.notificationManager.notify(TAG, notificationId, notification)

            if (!context.powerManager.isInteractiveCompat && wakeScreen) {
                //turn on screen when turned off
                val wakelock = context.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "omnitrack:ReminderScreenLock")
                wakelock.acquire(1000)
                val cpuWakelock = context.powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "omnitrack:ReminderScreenLockCpu")
                cpuWakelock.acquire(1000)
            }
        }

        if (expireAt < Long.MAX_VALUE) reserveAutoExpiry(entry)

    }

    internal fun dismissSyncImpl(entryId: Long, realm: Realm): Boolean {
        val entry = realm.where(OTTriggerReminderEntry::class.java)
                .equalTo("id", entryId).findFirst()
        if (entry != null) {
            realm.executeTransactionIfNotIn { realm ->
                entry.dismissed = true
            }
            context.notificationManager.cancel(TAG, entry.systemIntrinsicId)
            cancelAutoExpiryAlarm(entry)
            return true
        } else return false
    }

    internal fun restoreReminderNotifications(realm: Realm): Completable {
        return Completable.defer {
            val pendingEntries = realm.where(OTTriggerReminderEntry::class.java)
                    .equalTo("dismissed", false)
                    .sort("autoExpireAt", Sort.DESCENDING)
                    .findAll()
            pendingEntries.groupBy { entry -> entry.trackerId!! }
                    .forEach { (trackerId, entries) ->
                        if (entries.isNotEmpty()) {
                            if (entries.first().autoExpireAt >= System.currentTimeMillis()) {
                                val notiBuilder = buildNotificationFromEntry(entries.first(), null, null)
                                if (notiBuilder != null) {
                                    notifyNotification(entries.first(), notiBuilder.build(), false)
                                }
                            }
                        }
                    }

            return@defer Completable.complete()
        }

    }

    internal fun handlSystemRebootSyncImpl(realm: Realm) {


        val pendingEntries = realm.where(OTTriggerReminderEntry::class.java)
                .equalTo("dismissed", false)
                .sort("autoExpireAt", Sort.DESCENDING)
                .findAll()
        val entriesByTracker = pendingEntries.groupBy { entry -> entry.trackerId!! }
        realm.executeTransactionIfNotIn { realm ->
            entriesByTracker.forEach { (trackerId, entries) ->
                if (entries.isNotEmpty()) {
                    if (entries.first().autoExpireAt >= System.currentTimeMillis() + 5000) {
                        val tracker = dbManager.getTrackerQueryWithId(trackerId, realm).findFirst()
                        if (tracker != null) {
                            val entry = realm.createObject(OTTriggerReminderEntry::class.java, entryIdGenerator.getNewUniqueLong(System.currentTimeMillis()))
                            entry.level = OTReminderAction.NotificationLevel.Noti
                            entry.triggerId = entries.first().triggerId
                            entry.trackerId = trackerId
                            entry.autoExpireAt = entries.first().autoExpireAt
                            entry.serializedMetadata = entries.first().serializedMetadata

                            if (entries.first().autoExpireAt != Long.MAX_VALUE) {
                                entry.timeoutDuration = (entries.first().autoExpireAt - System.currentTimeMillis()).toInt()
                            }

                            entry.intrinsicTriggerTime = entries.first().intrinsicTriggerTime
                            entry.notifiedAt = System.currentTimeMillis()
                            val notificationId = notificationIdSeed.incrementAndGet()
                            entry.systemIntrinsicId = notificationId
                            val notiBuilder = makeReminderNotificationBuilderBase(notificationId, entry.triggerId!!, trackerId, tracker.name, entry.id, entry.intrinsicTriggerTime, entries.first().autoExpireAt, entry.timeoutDuration?.toLong(), entries.size, context.getString(R.string.msg_reminder_omitted))

                            if (entry.autoExpireAt < Long.MAX_VALUE) reserveAutoExpiry(entry)

                            context.runOnUiThread {
                                context.notificationManager.notify(TAG, notificationId, notiBuilder.build())
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
    }

    internal fun dismissAllReminders(realm: Realm, vararg triggers: OTTriggerDAO) {
        val entries = realm.where(OTTriggerReminderEntry::class.java)
                .`in`("triggerId", triggers.map { it._id }.toTypedArray())
                .equalTo("dismissed", false)
                .findAll()

        entries.groupBy { it.level }.forEach { (level, entriesInLevel) ->
            when (level) {
                OTReminderAction.NotificationLevel.Noti -> {
                    val notificationIds = entriesInLevel.map { it.systemIntrinsicId }
                    context.runOnUiThread {
                        notificationIds.forEach {
                            context.notificationManager.cancel(TAG, it)
                        }
                    }
                }
                else -> {
                    //TODO handle other reminder types
                }
            }
        }

        realm.executeTransactionIfNotIn {
            entries.deleteAllFromRealm()
        }
    }

    private fun insertNewReminderEntry(trigger: OTTriggerDAO, trackerId: String, action: OTReminderAction, triggerTime: Long, metadata: JsonObject, realm: Realm): OTTriggerReminderEntry {
        val entry = realm.createObject(OTTriggerReminderEntry::class.java, entryIdGenerator.getNewUniqueLong(System.currentTimeMillis()))
        entry.level = action.notificationLevelForSystem
        entry.triggerId = trigger._id
        entry.trackerId = trackerId
        entry.intrinsicTriggerTime = triggerTime
        entry.autoExpireAt = (trigger.action as? OTReminderAction)?.expiryMilliSeconds?.let { it + System.currentTimeMillis() } ?: Long.MAX_VALUE
        entry.timeoutDuration = (trigger.action as? OTReminderAction)?.expiryMilliSeconds
        entry.serializedMetadata = metadata.toString()

        entry.notifiedAt = System.currentTimeMillis()

        return entry
    }

    /**
     * reserve auto expiry. Don't need it from android O.
     */
    private fun reserveAutoExpiry(entry: OTTriggerReminderEntry) {
        if (Build.VERSION.SDK_INT < 26) {
            if (entry.timeoutDuration != null) {
                /*
            WorkManager.getInstance()!!.enqueue(OneTimeWorkRequestBuilder<ReminderDismissWorker>()
                    .setInitialDelay(entry.timeoutDuration!!.toLong(), TimeUnit.MILLISECONDS)
                    .setInputData(Data.Builder()
                            .putString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, entry.triggerId)
                            .putLong(INTENT_EXTRA_ENTRY_ID, entry.id)
                            .putString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configuredContext.configuration.id)
                            .build())
                    .addTag(entry.id.toString())
                    .build())*/

                entry.realm.executeTransactionIfNotIn {
                    if (entry.autoExpiryAlarmId == null)
                        entry.autoExpiryAlarmId = System.currentTimeMillis().toInt()
                    entry.isAutoExpiryAlarmReservedWhenDeviceActive = OTDeviceStatusService.isBatteryCharging(context) || context.powerManager.isInteractiveCompat
                }

                val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
                intent.action = OTApp.BROADCAST_ACTION_REMINDER_AUTO_EXPIRY_ALARM
                intent.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, entry.triggerId)
                intent.putExtra(OTReminderService.INTENT_EXTRA_ENTRY_ID, entry.id)
                val alarmIntent = PendingIntent.getBroadcast(context, entry.autoExpiryAlarmId!!, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                AlarmManagerCompat.setExactAndAllowWhileIdle(context.alarmManager, AlarmManager.RTC_WAKEUP, entry.notifiedAt + entry.timeoutDuration!!, alarmIntent)
            }
        }
    }

    private fun cancelAutoExpiryAlarm(entry: OTTriggerReminderEntry) {
        if (Build.VERSION.SDK_INT < 26) {
            entry.autoExpiryAlarmId?.let { alarmId ->
                val intent = Intent(context, TimeTriggerAlarmReceiver::class.java)
                intent.action = OTApp.BROADCAST_ACTION_REMINDER_AUTO_EXPIRY_ALARM
                val alarmIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                context.alarmManager.cancel(alarmIntent)
                alarmIntent.cancel()
            }
        }
    }

    fun rearrangeAutoExpiryAlarms(realm: Realm): Int {
        if (Build.VERSION.SDK_INT < 26) {
            val entries = realm.where(OTTriggerReminderEntry::class.java)
                    .isNotNull(OTTriggerReminderEntry.FIELD_AUTO_EXPIRY_ALARM_ID)
                    .equalTo(OTTriggerReminderEntry.FIELD_IS_AUTO_EXPIRY_ALARM_RESERVED_WHEN_DEVICE_ACTIVE, false)
                    .findAll()

            entries.forEach {
                reserveAutoExpiry(it)
            }

            OTApp.logger.writeSystemLog("Rearranged ${entries.size} auto expiry logs.", TAG)

            return entries.size
        } else return 0
    }


    private fun makeReminderNotificationBuilderBase(notiId: Int, triggerId: String, trackerId: String, trackerName: String, entryId: Long, reminderTime: Long, expireAt: Long, durationMs: Long?, dismissedCount: Int, title: CharSequence): NotificationCompat.Builder {

        println("reminderTime: $reminderTime, expireAt: $expireAt")
        /*
        val stackBuilder = TaskStackBuilder.create(this)
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(AItemDetailActivity::class.java)
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(AItemDetailActivity.makeIntent(trackerId, reminderTime, this))
        val resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT)
*/
        val accessPendingIntent = PendingIntent.getService(context, notiId, OTReminderService.makeReminderAccessedIntent(context, triggerId, trackerId, entryId, reminderTime), PendingIntent.FLAG_UPDATE_CURRENT)
        val dismissIntent = PendingIntent.getService(context, notiId,
                OTReminderService.makeReminderDismissedIntent(context, triggerId, entryId),
                PendingIntent.FLAG_UPDATE_CURRENT)

        val ringtone = pref.getString(SettingsActivity.PREF_REMINDER_NOTI_RINGTONE, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString())
        val lightColor = pref.getInt(SettingsActivity.PREF_REMINDER_LIGHT_COLOR, ContextCompat.getColor(context, R.color.colorPrimary))
        val contentTextBase = String.format(context.getString(R.string.msg_noti_tap_for_tracking_format), trackerName)

        val contentText: String = if (expireAt < Long.MAX_VALUE) {
            contentTextBase + " (${String.format(context.getString(R.string.msg_noti_reminder_until), dateFormat.format(Date(expireAt)))})"
        } else contentTextBase

        return OTTrackingNotificationFactory.makeBaseBuilder(context, System.currentTimeMillis(), OTNotificationManager.CHANNEL_ID_IMPORTANT)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(Uri.parse(
                        ringtone
                ))
                .setLights(lightColor, 1000, 500)
                //.setContentText(if(dismissedCount==0) contentTextBase else "$contentTextBase (${resources.getQuantityString(R.plurals.msg_reminder_omitted, dismissedCount)})")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(contentText)
                        .setBigContentTitle(title))
                .setContentTitle(title)
                .setAutoCancel(false)
                .setOngoing(true)
                .apply { if (durationMs != null) this.setTimeoutAfter(durationMs) }
                .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.msg_reminder_open_input), accessPendingIntent).build())
                .addAction(NotificationCompat.Action.Builder(0, context.getString(R.string.msg_reminder_skip), dismissIntent).build())
                .setDeleteIntent(dismissIntent)
                .setContentIntent(accessPendingIntent)
        //.setFullScreenIntent(accessPendingIntent, true)

    }

}