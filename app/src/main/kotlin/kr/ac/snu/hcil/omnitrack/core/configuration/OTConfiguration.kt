package kr.ac.snu.hcil.omnitrack.core.configuration

import android.net.Uri
import kr.ac.snu.hcil.omnitrack.BuildConfig
import okhttp3.HttpUrl

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

    var synchronizationServerUrl: String by table; private set
    var mediaStorageServerUrl: String by table; private set
    var firebaseCloudMessagingSenderId: String by table; private set
    var firebaseCrashReportingApiKey: String by table; private set
    var firebaseGoogleApiKey: String by table; private set
    var firebaseProjectId: String by table; private set
    var googleAuthClientId: String by table; private set

    init {
        synchronizationServerUrl = BuildConfig.OMNITRACK_SYNCHRONIZATION_SERVER_URL
        mediaStorageServerUrl = BuildConfig.OMNITRACK_MEDIA_STORAGE_SERVER_URL
        firebaseCloudMessagingSenderId = BuildConfig.FIREBASE_CLOUD_MESSAGING_SENDER_ID
        firebaseCrashReportingApiKey = BuildConfig.FIREBASE_API_KEY
        firebaseGoogleApiKey = BuildConfig.FIREBASE_API_KEY
        firebaseProjectId = BuildConfig.FIREBASE_PROJECT_ID
        googleAuthClientId = BuildConfig.FIREBASE_AUTH_CLIENT_ID
        process()
    }

    private fun process() {
        synchronizationServerUrl = fallbackUrl(synchronizationServerUrl)
        mediaStorageServerUrl = fallbackUrl(mediaStorageServerUrl)
    }
}