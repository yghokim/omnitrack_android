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
import dagger.internal.Factory
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.configuration.OTConfigurationController
import kr.ac.snu.hcil.omnitrack.core.database.global.OTAttachedConfigurationDao
import kr.ac.snu.hcil.omnitrack.core.di.global.AppLevelDatabase
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.system.OTNotificationManager
import kr.ac.snu.hcil.omnitrack.ui.pages.home.HomeActivity
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import org.jetbrains.anko.notificationManager
import java.lang.Exception
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
    }

    @Inject
    lateinit var dispatcher: FirebaseJobDispatcher

    @Inject
    lateinit var configController: OTConfigurationController

    @field:[Inject AppLevelDatabase]
    lateinit var realmFactory: Factory<Realm>

    private val subscriptions = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscriptions.clear()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val senderId = remoteMessage.from
        val receiverToken = remoteMessage.to

        println("OTFirebaseMessagingService: received Firebase Cloud message. Sender: $senderId | Receiver: $receiverToken")
        if (senderId != null) {
            subscriptions.add(
                    Completable.defer {
                        realmFactory.get().use { realm ->
                            val dao = realm.where(OTAttachedConfigurationDao::class.java)
                                    .equalTo(OTAttachedConfigurationDao.FIELD_GCM_SENDER_ID, senderId).findFirst()
                            if (dao != null) {
                                println("OTFirebaseMessagingService: Found corresponding configuration context sent from: ${senderId}")
                                val configuredContext = configController.getConfiguredContextOf(dao.staticConfiguration())
                                if (configuredContext != null) {
                                    val data = remoteMessage.data
                                    if (data != null && data.size > 0) {

                                        configuredContext.configuredAppComponent.getEventLogger().logEvent(TAG, "received_gcm_command", jsonObject("command" to data.get("command")))
                                        try {
                                            when (data.get("command")) {
                                                COMMAND_SYNC -> handleSyncCommand(data, configuredContext)
                                                COMMAND_SIGNOUT -> handleSignOutCommand(data, configuredContext)
                                                COMMAND_DUMP_DB -> handleDumpCommand(data, configuredContext)
                                                COMMAND_TEXT_MESSAGE -> handleMessageCommand(data, configuredContext)
                                            }
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }
                                }
                            } else {
                                println("OTFirebaseMessagingService: Did not find any corresponding configuration with sender Id")
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
}