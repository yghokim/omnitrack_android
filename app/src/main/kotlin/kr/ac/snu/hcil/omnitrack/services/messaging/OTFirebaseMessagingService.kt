package kr.ac.snu.hcil.omnitrack.services.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.auth.OTAuthManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 12..
 */
class OTFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val COMMAND_SYNC = "sync_down"
        const val COMMAND_SIGNOUT = "sign_out"
        const val COMMAND_DUMP_DB = "dump_db"
    }

    @Inject
    lateinit var syncManager: OTSyncManager

    @Inject
    lateinit var authManager: OTAuthManager

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        println("received Firebase Cloud message")
        val data = remoteMessage.data
        if (data != null && data.size > 0) {
            try {
                when (data.get("command")) {
                    COMMAND_SYNC -> handleSyncCommand(data)
                    COMMAND_SIGNOUT -> handleSignOutCommand(data)
                    COMMAND_DUMP_DB -> handleDumpCommand(data)
                }
            } catch (ex: Exception) {

            }
        }
    }

    private fun handleSyncCommand(data: Map<String, String>) {
        println("received Firebase Cloud message - sync")
        val syncInfoArray = Gson().fromJson<JsonArray>(data.get("syncInfoArray"), JsonArray::class.java)
        var registeredCount = 0
        syncInfoArray.forEach { syncInfo ->
            val typeString = (syncInfo as JsonObject).get("type")?.asString
            if (typeString != null) {
                syncManager.registerSyncQueue(ESyncDataType.valueOf(typeString), SyncDirection.DOWNLOAD, false)
                registeredCount++
            }
        }

        if (registeredCount > 0) {
            syncManager.reserveSyncServiceNow()
        }
    }

    private fun handleDumpCommand(data: Map<String, String>) {

    }

    private fun handleSignOutCommand(data: Map<String, String>) {
        authManager.signOut()
    }
}