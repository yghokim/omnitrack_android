package kr.ac.snu.hcil.omnitrack.core.configuration

import android.net.Uri
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.utils.parseToMap
import okhttp3.HttpUrl
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Young-Ho on 12/9/2017.
 */
class OTConfiguration {

    companion object {
        private fun fallbackUrl(url: String): String {
            val syncServerUrl = HttpUrl.parse(url)
            val result = if (syncServerUrl == null) {
                Uri.Builder().path(url).scheme("http").build().toString()
            } else url

            println("fallback url: ${result}")

            return result
        }
    }

    private val table: MutableMap<String, Any?> = HashMap()

    var id: String by table; private set
    var synchronizationServerUrl: String by table; private set
    var mediaStorageServerUrl: String by table; private set
    var firebaseCloudMessagingSenderId: String by table; private set
    var firebaseCrashReportingApiKey: String by table; private set
    var firebaseGoogleApiKey: String by table; private set
    var firebaseProjectId: String by table; private set
    var googleAuthClientId: String by table; private set

    constructor() {
        id = UUID.randomUUID().toString()
        synchronizationServerUrl = BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL
        mediaStorageServerUrl = BuildConfig.OMNITRACK_MEDIA_STORAGE_SERVER_URL
        firebaseCloudMessagingSenderId = BuildConfig.FIREBASE_CLOUD_MESSAGING_SENDER_ID
        firebaseCrashReportingApiKey = BuildConfig.FIREBASE_API_KEY
        firebaseGoogleApiKey = BuildConfig.FIREBASE_API_KEY
        firebaseProjectId = BuildConfig.FIREBASE_PROJECT_ID
        googleAuthClientId = BuildConfig.FIREBASE_AUTH_CLIENT_ID

        process()
    }

    constructor(jsonString: String) {
        println("deserialize OTConfiguration JsonString: ${jsonString}")
        this.table.putAll(OTApp.instance.applicationComponent.genericGson().parseToMap(jsonString))

        process()
    }

    private fun process() {
        synchronizationServerUrl = fallbackUrl(synchronizationServerUrl)
        mediaStorageServerUrl = fallbackUrl(mediaStorageServerUrl)
    }

    fun toJson(): String {
        val jsonString = OTApp.instance.applicationComponent.genericGson().toJson(table)
        println("OTConfiguration json: ${jsonString}")
        return jsonString
    }
}