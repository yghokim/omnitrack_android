package kr.ac.snu.hcil.omnitrack.services.messaging

import android.app.PendingIntent
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.github.salomonbrys.kotson.jsonObject
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.core.database.configured.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import org.jetbrains.anko.notificationManager
import java.io.IOException
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 12..
 */
class OTFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "OTFirebaseMessagingService"

        const val COMMAND_TEXT_MESSAGE = "text_message"

        const val COMMAND_SYNC = "sync_down"
        const val COMMAND_SIGNOUT = "sign_out"
        const val COMMAND_DUMP_DB = "dump_db"

        const val COMMAND_NEW_UPDATE_RELEASED = "update_released"
        const val COMMAND_EXPERIMENT_DROPPED = "experiment_dropped"

        const val COMMAND_TEST_TRIGGER_PING = "test_trigger_ping"
    }

    @Inject
    lateinit var dispatcher: FirebaseJobDispatcher

    @Inject
    lateinit var configController: OTConfigurationController

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTAndroidApp).applicationComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        subscriptions.add(
                Completable.merge(
                        configController.map { config ->
                            Completable.defer {
                                val configuredContext = configController.currentConfiguredContext
                                try {
                                    val fbInstanceId = configuredContext.firebaseComponent.getFirebaseInstanceId()
                                    val token = fbInstanceId.getToken(config.firebaseCloudMessagingSenderId, "FCM")
                                    if (token != null) {
                                        println("FirebaseInstanceId token - $token")
                                        val currentUserId = configuredContext.configuredAppComponent.getAuthManager().userId
                                        if (currentUserId != null) {
                                            val jobBuilder = configuredContext.scheduledJobComponent.getInformationUploadJobBuilderProvider().get()
                                            dispatcher.mustSchedule(jobBuilder.build())
                                        }
                                    }
                                } catch (ex: IOException) {
                                    ex.printStackTrace()
                                }
                                return@defer Completable.complete()
                            }
                        }
                ).subscribeOn(Schedulers.io())
                        .subscribe({
                            println("Firebase Instance Id Token was refreshed: onTokenRefresh")
                        }, { ex ->
                            ex.printStackTrace()
                        })
        )
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val senderId = remoteMessage.from
        val receiverToken = remoteMessage.to

        println("OTFirebaseMessagingService: received Firebase Cloud message. Sender: $senderId | Receiver: $receiverToken")
        if (senderId != null) {
            subscriptions.add(
                    Completable.defer {
                        val configuredContext = configController.currentConfiguredContext
                        val data = remoteMessage.data
                        if (data != null && data.isNotEmpty()) {

                            configuredContext.configuredAppComponent.getEventLogger().logEvent(TAG, "received_gcm_command", jsonObject("command" to data.get("command")))
                            try {
                                when (data.get("command")) {
                                    COMMAND_SYNC -> handleSyncCommand(data, configuredContext)
                                    COMMAND_SIGNOUT -> handleSignOutCommand(data, configuredContext)
                                    COMMAND_DUMP_DB -> handleDumpCommand(data, configuredContext)
                                    COMMAND_EXPERIMENT_DROPPED -> handleExperimentDropout(data, configuredContext)
                                    COMMAND_TEXT_MESSAGE -> handleMessageCommand(data, configuredContext)
                                    COMMAND_TEST_TRIGGER_PING -> handleTestTriggerPing(data, configuredContext)
                                }
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                        return@defer Completable.complete()
                    }.subscribeOn(Schedulers.io()).subscribe {

                    }
            )
        }
    }

    private fun handleSyncCommand(data: Map<String, String>, configuredContext: ConfiguredContext) {
        println("received Firebase Cloud message - sync")
        val syncInfoArray = Gson().fromJson<JsonArray>(data.get("syncInfoArray"), JsonArray::class.java)
        var registeredCount = 0
        syncInfoArray.forEach { syncInfo ->
            val typeString = (syncInfo as JsonObject).get("type")?.asString
            if (typeString != null) {
                configuredContext.configuredAppComponent.getSyncManager().registerSyncQueue(ESyncDataType.valueOf(typeString), SyncDirection.DOWNLOAD, false, false)
                registeredCount++
            }
        }

        if (registeredCount > 0) {
            configuredContext.configuredAppComponent.getSyncManager().reserveSyncServiceNow()
        }
    }

    private fun handleDumpCommand(data: Map<String, String>, configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.getSyncManager().queueFullSync(SyncDirection.UPLOAD, true)
        configuredContext.configuredAppComponent.getSyncManager().reserveSyncServiceNow()
    }

    private fun handleSignOutCommand(data: Map<String, String>, configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.getAuthManager().signOut()
    }

    private fun handleExperimentDropout(data: Map<String, String>, configuredContext: ConfiguredContext) {
        println("experiment dropout message received")
        if (data.containsKey("experimentId")) {
            val experimentId = data["experimentId"]!!
            if (BuildConfig.DEFAULT_EXPERIMENT_ID == experimentId) {
                configuredContext.configuredAppComponent.getAuthManager().signOut()
            }
        } else {
            println("experiment dropout message does not contain the experiment ID.")
        }
    }

    private fun handleMessageCommand(data: Map<String, String>, configuredContext: ConfiguredContext) {
        val title = data.get("messageTitle")
        val content = data.get("messageContent")
        val contentHtml = TextHelper.fromHtml(content ?: "")

        val notificationBuilder = NotificationCompat.Builder(this, OTNotificationManager.CHANNEL_ID_IMPORTANT)
                .setAutoCancel(true)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSmallIcon(R.drawable.icon_simple)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, HomeActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(title)
                .setContentText(contentHtml)
                .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(contentHtml))

        notificationManager.notify(TAG, System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun handleTestTriggerPing(data: Map<String, String>, configuredContext: ConfiguredContext) {
        val triggerId = data.get("triggerId")
        val userId = configuredContext.configuredAppComponent.getAuthManager().userId
        println("received a test trigger ping - ${triggerId}")
        if (triggerId != null && triggerId.isNotBlank() && userId != null) {
            configuredContext.configuredAppComponent.backendRealmFactory().get().use { realm ->
                val trigger = configuredContext.configuredAppComponent.getBackendDbManager().getTriggerQueryWithId(triggerId, realm).equalTo(BackendDbManager.FIELD_USER_ID, userId).findFirst()
                trigger?.getPerformFireCompletable(System.currentTimeMillis(), jsonObject("source" to "ping_test"), configuredContext)?.blockingAwait()
            }

        }
    }
}