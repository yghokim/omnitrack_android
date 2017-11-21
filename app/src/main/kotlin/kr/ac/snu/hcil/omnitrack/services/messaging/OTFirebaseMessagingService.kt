package kr.ac.snu.hcil.omnitrack.services.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import javax.inject.Inject

/**
 * Created by younghokim on 2017. 4. 12..
 */
class OTFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val COMMAND_SYNC = "kr.ac.snu.hcil.omnitrack.messaging.sync_down"
    }

    @Inject
    lateinit var syncManager: OTSyncManager

    override fun onCreate() {
        super.onCreate()
        (application as OTApp).applicationComponent.inject(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        println("received Firebase Cloud message")
        println(remoteMessage)
        if (remoteMessage.data.size > 0) {
            try {
                if (remoteMessage.data.get("command") == COMMAND_SYNC) {
                    //synchronization
                    println("received Firebase Cloud message - sync")
                    val syncInfoArray = Gson().fromJson<JsonArray>(remoteMessage.data.get("syncInfoArray"), JsonArray::class.java)
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
            } catch (ex: Exception) {

            }
        }
    }
}