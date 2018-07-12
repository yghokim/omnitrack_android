package kr.ac.snu.hcil.omnitrack.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.triggers.OTReminderCommands
import kr.ac.snu.hcil.omnitrack.utils.ConfigurableWakefulService

/**
 * Created by younghokim on 2017. 11. 13..
 */
class OTReminderService : ConfigurableWakefulService(TAG) {

    companion object {
        const val TAG = "OTReminderService"
        const val ACTION_PREFIX = "${BuildConfig.APPLICATION_ID}.${TAG}.action"
        //const val ACTION_REMIND = "${ACTION_PREFIX}.remind"
        const val ACTION_ON_USER_ACCESS = "${ACTION_PREFIX}.user_accessed"
        const val ACTION_ON_USER_DISMISS = "${ACTION_PREFIX}.dismissed"
        const val ACTION_ON_USER_LOGGED = "${ACTION_PREFIX}.user_logged"

        const val INTENT_EXTRA_ENTRY_ID = "entryId"
        const val INTENT_EXTRA_LOGGED_AT = "loggedAt"

        private fun makeBaseIntent(context: Context, configId: String, action: String): Intent {
            return Intent(context, OTReminderService::class.java)
                    .setAction(action)
                    .putExtra(OTApp.INTENT_EXTRA_CONFIGURATION_ID, configId)
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
    }

    inner class ConfiguredTask(startId: Int, configuredContext: ConfiguredContext) : AConfiguredTask(startId, configuredContext) {

        private val commands = OTReminderCommands(configuredContext, this@OTReminderService)
        private val subscriptions = CompositeDisposable()

        override fun dispose() {
            subscriptions.clear()
        }

        override fun onStartCommand(intent: Intent, flags: Int): Int {
            OTApp.logger.writeSystemLog("Start OTReminderService with command ${intent.action}", TAG)
            val completable = when (intent.action) {
            /*ACTION_REMIND -> commands.remind(
                    intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                    intent.getLongExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())
            )*/
                ACTION_ON_USER_ACCESS -> {
                    commands.onUserAccessed(startId,
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER),
                            intent.getLongExtra(INTENT_EXTRA_ENTRY_ID, 0),
                            intent.getLongExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, System.currentTimeMillis())
                    )
                }
                ACTION_ON_USER_DISMISS -> {
                    commands.onUserDismissed(startId,
                            intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER),
                            intent.getLongExtra(INTENT_EXTRA_ENTRY_ID, 0)
                    )
                }
                ACTION_ON_USER_LOGGED -> {
                    commands.onUserLogged(startId, intent.getStringExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRACKER),
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
                            .subscribe({
                                OTApp.logger.writeSystemLog("Successfully handled reminder service action.", TAG)
                            }, { error ->
                                OTApp.logger.writeSystemLog("Error while handling the reminder service action - ${intent.action},\n${Log.getStackTraceString(error)}", TAG)
                                error.printStackTrace()
                            })
            )

            return START_NOT_STICKY
        }

    }

    override fun onInject(app: OTApp) {
        app.applicationComponent.inject(this)
    }

    override fun makeConfiguredTask(startId: Int, configuredContext: ConfiguredContext): AConfiguredTask {
        return ConfiguredTask(startId, configuredContext)
    }

    class ReminderDismissWorker : Worker() {

        override fun doWork(): Result {
            return try {
                //val triggerId = inputData.getString(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, "")
                val entryId = inputData.getLong(INTENT_EXTRA_ENTRY_ID, 0)
                val configId = inputData.getString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, "")!!

                val configuredContext = (applicationContext as OTApp).applicationComponent.configurationController().getConfiguredContextOf(configId)
                if (configuredContext != null) {
                    //this.applicationContext.startService(OTReminderService.makeReminderDismissedIntent(this.applicationContext, configId, triggerId, entryId))
                    val commands = OTReminderCommands(configuredContext, applicationContext)

                    val realm = commands.getNewRealm()
                    commands.dismissSyncImpl(entryId, realm)
                    realm.close()
                    OTApp.logger.writeSystemLog("Successfully dismissed reminder by Worker. entryId: $entryId", "ReminderDismissWorker")
                    Result.SUCCESS
                } else Result.FAILURE
            } catch (ex: Exception) {
                OTApp.logger.writeSystemLog("ReminderDismissWorker doWork error: \n${Log.getStackTraceString(ex)}", "ReminderDismissWorker")
                Result.FAILURE
            }
        }
    }

    class SystemRebootWorker : Worker() {
        override fun doWork(): Result {
            return try {
                val configId = inputData.getString(OTApp.INTENT_EXTRA_CONFIGURATION_ID, "")!!

                val configuredContext = (applicationContext as OTApp).applicationComponent.configurationController().getConfiguredContextOf(configId)
                if (configuredContext != null) {
                    //this.applicationContext.startService(OTReminderService.makeReminderDismissedIntent(this.applicationContext, configId, triggerId, entryId))
                    val commands = OTReminderCommands(configuredContext, applicationContext)
                    val realm = commands.getNewRealm()
                    commands.handlSystemRebootSyncImpl(realm)
                    realm.close()
                    OTApp.logger.writeSystemLog("Successfully handled reboot for reminder by Worker.", "OTReminderService.SystemRebootWorker")
                    Result.SUCCESS
                } else Result.FAILURE
            } catch (ex: Exception) {
                OTApp.logger.writeSystemLog("Reminder Reboot Handling failed: \n ${Log.getStackTraceString(ex)}", "OTReminderService.SystemRebootWorker")
                Result.RETRY
            }
        }
    }
}